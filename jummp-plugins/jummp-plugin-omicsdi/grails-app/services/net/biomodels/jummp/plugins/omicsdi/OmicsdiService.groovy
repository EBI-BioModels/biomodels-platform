/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 **/





package net.biomodels.jummp.plugins.omicsdi

import grails.util.Environment
import groovy.json.JsonBuilder
import net.biomodels.jummp.core.constants.BioModels
import net.biomodels.jummp.core.constants.Redis
import org.perf4j.aop.Profiled

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

/**
 * @short Omicsdi class for managing OmicsDI's settings
 *
 * This class provides means of handling functionality for searching models based on OmicsDI's API.
 * It accesses the database and get essential data that are included into XML files.
 * Otherwise, we implement the methods configuring OmicsDI API.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */

class OmicsdiService {
    private final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    private final boolean IS_ERROR_ENABLED = log.isErrorEnabled()
    private final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * Dependency Injection of Metadata Delegate Service
     **/
    def metadataDelegateService
    /**
     * Dependency Injection of Spring Security Service
     **/
    def springSecurityService
    /**
     * Dependency Injection of Grails Application
     **/
    def grailsApplication
    /*
     * Dependency injection of the configuration service
     */
    def configurationService
    /**
     * Dependency Injection of Redis Service, used to flag that a Model/Revision change happened
     * since the last OmicsDI export so the daily archive job knows whether there is anything to do.
     **/
    def redisService

    // TODO: Define an OmicsDI XML based schema from OmicsDI specification, instead of holding a class
    static final String GRAILS_CONF_LOCATION = "grails-app/conf"
    static final String OMICSDI_CONFIG_LOCATION = "omicsdi"

    /**
     * Matches the hand-maintained {@code omicsdi-config.json} that has always worked when the
     * indexer is run by hand. {@code numberEntriesOnEachFile} must be a positive integer whenever
     * {@code allowMultipleFiles} is true (the indexer casts it straight to {@code Integer});
     * {@code tagsExcluded} is the curation policy for the unattended daily export.
     */
    static final int DEFAULT_ENTRIES_PER_FILE = 1000
    static final List<String> DEFAULT_TAGS_EXCLUDED = ['Auto-Generated', 'Uhlen'].asImmutable()

    @Profiled(tag="omicsdiService.loadSchemaXml")
    String loadSchemaXml() {
        return GRAILS_CONF_LOCATION.concat("/").concat(OMICSDI_CONFIG_LOCATION)
    }

    File saveOmicsdiExportSettings(def options) {
        def dsConfig = grailsApplication.config.dataSource
        String searchStrategy = grailsApplication.config.jummp.search.strategy
        String exportFolder = grailsApplication.config.jummp.search.exportFolder
        String dbUrl = rewriteJdbcUrlForIndexer(dsConfig?.url as String)
        String dbUsername = dsConfig?.username
        String dbPassword = dsConfig?.password
        def dbSettings = ['url': dbUrl, 'username': dbUsername, 'password': dbPassword]
        def builder = new JsonBuilder()
        String serverUrl = BioModels.BM_ROOT_URL
        if (Environment.current != Environment.PRODUCTION) {
            serverUrl = grailsApplication.config.grails.serverURL
        }
        def partialData = [
            'repositoryName' : grailsApplication.config.jummp.metadata.officialDatabaseName,
            'serverUrl' : serverUrl
        ]
        builder(partialData: partialData,
            'folder': exportFolder,
            // The indexer refreshes miriam.xml at this path, and - more importantly - its
            // RequestContext constructor throws when the key is absent, which is what silently
            // aborted every app-triggered export (the hand-maintained omicsdi-config.json always
            // carried it, so `run.sh` was fine).
            'miriamExportFile': new File(exportFolder as String, "miriam.xml").path,
            'jummpPropFile': configurationService.getConfigFilePath(),
            'searchStrategy': searchStrategy,
            'database': dbSettings,
            'options': normaliseExportOptions(options))
        File indexingData = new File(exportFolder, "omicsdiSettings.json")
        indexingData.setText(builder.toPrettyString())
        return indexingData
    }

    void exportOmicsdiEntries(Map options = [:]) {
        Map exportOptions = [
            'allowMultipleFiles': true,
            'numberEntriesOnEachFile': DEFAULT_ENTRIES_PER_FILE,
            'tagsExcluded': new ArrayList<String>(DEFAULT_TAGS_EXCLUDED)
        ] + options
        File indexingData = saveOmicsdiExportSettings(exportOptions)
        String jarJummpIndexerPath = grailsApplication.config.jummp.search.pathToIndexerExecutable

        def argsMap = [
            jarPath: jarJummpIndexerPath,
           jsonPath: indexingData.getCanonicalPath(),
           omicsdi: "OmicsDIXml"
        ]

        argsMap.putAll(configurationService.configureProxySettings() as Map<? extends String, ? extends String>)

        try {
            sendMessage("seda:omicsDiExport", argsMap)
        } catch (Exception e) {
            if (IS_ERROR_ENABLED) {
                log.error("Failed to build OmicsDI XML based schema files - ${e.message}", e)
            }
        }
    }

    /**
     * Flags that a Model/Revision has changed since the last archived export, so that
     * {@link net.biomodels.jummp.plugins.omicsdi.OmicsdiGitExportJob} knows there is something
     * to export the next time it runs.
     */
    void markExportDirty() {
        redisService.doRedisSet(Redis.REDIS_KEY_OMICSDI_EXPORT_DIRTY, new Date().format("yyyy-MM-dd'T'HH:mm:ssZ"))
    }

    boolean isExportDirty() {
        redisService.exists(Redis.REDIS_KEY_OMICSDI_EXPORT_DIRTY)
    }

    private void clearExportDirty() {
        redisService.doRedisDel(Redis.REDIS_KEY_OMICSDI_EXPORT_DIRTY)
    }

    /**
     * Copies the XML files that the indexer jar just wrote into {@code jummp.search.exportFolder}
     * into the local clone of the git archive repo, then commits + pushes them.
     *
     * Called asynchronously by {@code ExportingOmicsDIRoute} (grails-app/routes) as the last
     * step of the {@code seda:omicsDiExport} Camel route, i.e. strictly after the indexer jar
     * (invoked via {@code exec:java}, {@code jummp.search.pathToIndexerExecutable}) has finished
     * writing the XML files - never called directly/synchronously by request-handling code.
     *
     * @return true if the archive repo ended up up to date (including the "nothing changed" case),
     *         false if a step failed - the dirty flag is left set so the next scheduled export retries.
     */
    boolean archiveExportedXmlToGit() {
        if (!(grailsApplication.config.jummp.omicsdi.git.enabled)) {
            log.warn("jummp.omicsdi.git.repoPath is not configured; skipping the OmicsDI git archive step.")
            return false
        }
        File repoDir = new File(stripQuotes(grailsApplication.config.jummp.omicsdi.git.repoPath as String))
        String subdir = stripQuotes(grailsApplication.config.jummp.omicsdi.git.subdir as String)
        // An explicit empty value in the external .properties config (jummp.omicsdi.git.subdir=)
        // gets past Config.groovy's ConfigObject fallback as "". `new File(repoDir, "")` still
        // resolves to the repo root, but `git add -- ""` / `git status -- ""` abort with
        // "empty string is not a valid pathspec" (git >= 2.16), so normalise it to ".".
        if (!subdir?.trim()) {
            subdir = "."
        }
        String remote = stripQuotes(grailsApplication.config.jummp.omicsdi.git.remote as String)
        String branch = stripQuotes(grailsApplication.config.jummp.omicsdi.git.branch as String)
        String exportFolder = grailsApplication.config.jummp.search.exportFolder as String

        // Never let a misconfigured repoPath (e.g. one with stray quote characters, which makes
        // it a relative path) fall through to mkdir'ing/committing/pushing inside whatever the
        // JVM's working directory happens to be - for a Grails app run from its own checkout,
        // that's this very repo. Refuse to proceed unless repoPath unambiguously points at an
        // existing git working copy.
        if (!repoDir.isAbsolute() || !new File(repoDir, ".git").exists()) {
            log.error("jummp.omicsdi.git.repoPath (`${repoDir}`) is not an absolute path to an " +
                "existing git repository; refusing to touch it. Skipping the OmicsDI git archive step.")
            return false
        }

        try {
            File targetDir = new File(repoDir, subdir)
            targetDir.mkdirs()
            int copied = copyXmlFilesIntoRepo(new File(exportFolder), targetDir)
            log.info("Copied ${copied} OmicsDI XML file(s) into ${targetDir.canonicalPath}.")

            runCommand(["git", "add", "-A", "--", subdir], repoDir)
            String status = runCommand(["git", "status", "--porcelain", "--", subdir], repoDir)
            if (!status?.trim()) {
                log.info("No OmicsDI XML changes to archive today.")
                clearExportDirty()
                return true
            }

            List<String> commitCommand = [
                "git",
                "-c", "user.name=${grailsApplication.config.jummp.omicsdi.git.commitUserName}".toString(),
                "-c", "user.email=${grailsApplication.config.jummp.omicsdi.git.commitUserEmail}".toString(),
                "commit", "-m", "Automated OmicsDI export - ${new Date().format('yyyy-MM-dd')}".toString()
            ]
            runCommand(commitCommand, repoDir)
            runCommand(["git", "push", remote, branch], repoDir)

            log.info("Archived and pushed today's OmicsDI export to ${remote}/${branch}.")
            clearExportDirty()
            return true
        } catch (Exception e) {
            log.error("Failed to archive OmicsDI XML files to git: ${e.message}", e)
            return false
        }
    }

    /**
     * Brings an OmicsDI export options map into a shape the indexer jar can actually consume.
     *
     * When {@code allowMultipleFiles} is true the indexer casts {@code numberEntriesOnEachFile}
     * straight to {@code Integer}. The admin form submits the string {@code "undefined"} when that
     * field is left blank ({@link OmicsdiController#parseOptions}), and that used to be the default
     * here too, so the cast threw {@code NumberFormatException} and the run died before writing a
     * single XML file. Coerce it to a positive integer, falling back to
     * {@link #DEFAULT_ENTRIES_PER_FILE}.
     */
    private static Map normaliseExportOptions(def options) {
        Map opts = (options instanceof Map) ? new LinkedHashMap(options as Map) : [:]
        if (opts.allowMultipleFiles) {
            def raw = opts.numberEntriesOnEachFile
            Integer entries = null
            if (raw instanceof Number) {
                entries = (raw as Number).intValue()
            } else if ((raw as String)?.isInteger()) {
                entries = (raw as String).toInteger()
            }
            opts.numberEntriesOnEachFile = (entries != null && entries > 0) ? entries : DEFAULT_ENTRIES_PER_FILE
        }
        opts
    }

    private static final String MARIADB_JDBC_PREFIX = "jdbc:mariadb:"
    private static final String MYSQL_JDBC_PREFIX = "jdbc:mysql:"

    /**
     * Rewrites a {@code jdbc:mariadb:...} datasource URL to its {@code jdbc:mysql:...} equivalent
     * before it is handed to the standalone indexer jar ({@code jummp.search.pathToIndexerExecutable},
     * invoked via {@code exec:java} in {@code ExportingOmicsDIRoute}).
     *
     * That jar bundles only the MySQL Connector/J driver - it has no {@code org.mariadb.jdbc.Driver}
     * on its classpath - so a {@code jdbc:mariadb:} URL (what {@code DataSource.groovy} builds when
     * {@code jummp.database.type=MARIADB}) makes it abort at GORM startup with
     * {@code java.sql.SQLException: No suitable driver} and no XML is produced. Connector/J 8 speaks
     * to a MariaDB server over a {@code jdbc:mysql:} URL, and the query parameters our config appends
     * ({@code useUnicode}, {@code characterEncoding}) are understood by both drivers.
     *
     * Any other URL (mysql, postgresql, the in-memory h2 used by tests) is returned unchanged.
     */
    private static String rewriteJdbcUrlForIndexer(String url) {
        if (url?.startsWith(MARIADB_JDBC_PREFIX)) {
            return MYSQL_JDBC_PREFIX + url.substring(MARIADB_JDBC_PREFIX.length())
        }
        url
    }

    /**
     * Defensively strips one layer of matching leading/trailing quote characters. Unlike Groovy
     * ConfigSlurper (.groovy) config files, a plain .properties file does not strip quotes around
     * a value - `key="/some/path"` there ends up with the quote characters baked into the string
     * itself, silently turning an absolute path into a relative (and nonsensical) one.
     */
    private static String stripQuotes(String value) {
        if (value?.length() >= 2 &&
                ((value.startsWith('"') && value.endsWith('"')) ||
                 (value.startsWith("'") && value.endsWith("'")))) {
            return value[1..-2]
        }
        value
    }

    private static int copyXmlFilesIntoRepo(File exportFolder, File targetDir) {
        int copied = 0
        exportFolder.eachFileMatch(~/(?i).*\.xml$/) { File xmlFile ->
            Path destination = Paths.get(targetDir.canonicalPath, xmlFile.name)
            Files.copy(xmlFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING)
            copied++
        }
        copied
    }

    /**
     * Runs a command to completion, failing loudly (rather than hanging or silently swallowing
     * errors) since this is invoked from an unattended, scheduled context.
     *
     * The output stream is drained on a background thread so that a stuck child process (e.g. git
     * blocked on an interactive prompt) is still caught by the waitFor timeout below - reading the
     * stream inline would block this thread until the process exits, defeating the timeout.
     */
    private static String runCommand(List<String> command, File workingDir, long timeoutSeconds = 300) {
        ProcessBuilder builder = new ProcessBuilder(command)
        builder.directory(workingDir)
        builder.redirectErrorStream(true)
        builder.environment().put("GIT_TERMINAL_PROMPT", "0")
        Process process = builder.start()

        StringBuilder outputBuilder = new StringBuilder()
        Thread reader = Thread.start {
            try {
                process.inputStream.eachLine { String line -> outputBuilder.append(line).append('\n') }
            } catch (IOException ignored) {
                // stream was closed from under us, e.g. after destroyForcibly() on timeout
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            reader.join(5000)
            throw new IOException("Command `${command.join(' ')}` timed out after ${timeoutSeconds}s. Output so far: ${outputBuilder}")
        }
        reader.join(5000)
        String output = outputBuilder.toString()
        if (process.exitValue() != 0) {
            throw new IOException("Command `${command.join(' ')}` failed with exit code ${process.exitValue()}: ${output}")
        }
        output
    }
}
