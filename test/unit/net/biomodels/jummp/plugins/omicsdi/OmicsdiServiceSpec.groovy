package net.biomodels.jummp.plugins.omicsdi

import grails.test.mixin.TestFor
import groovy.json.JsonSlurper
import net.biomodels.jummp.core.constants.Redis
import net.biomodels.jummp.utils.redis.RedisService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions.
 *
 * RedisService's read/write methods are `static`, backed by a live Jedis pool, so a plain
 * per-instance closure reassignment (`redisService.doRedisSet = {...}`) doesn't intercept them -
 * that syntax only overrides instance methods. Static methods have to be stubbed on the class's
 * own metaClass instead (`RedisService.metaClass.static.doRedisSet = {...}`), which is reset in
 * cleanup() since it would otherwise leak into every other spec that runs in the same JVM.
 */
@TestFor(OmicsdiService)
class OmicsdiServiceSpec extends Specification {

    File tempDir

    def setup() {
        service.redisService = new RedisService()
        tempDir = File.createTempFile("omicsdi-service-spec", "")
        tempDir.delete()
        tempDir.mkdirs()
    }

    def cleanup() {
        tempDir?.deleteDir()
        GroovySystem.metaClassRegistry.removeMetaClass(RedisService)
    }

    void "markExportDirty writes a non-empty value to the omicsdi dirty-flag redis key"() {
        given:
        String capturedKey
        String capturedValue
        RedisService.metaClass.static.doRedisSet = { String key, String value ->
            capturedKey = key
            capturedValue = value
        }

        when:
        service.markExportDirty()

        then:
        capturedKey == Redis.REDIS_KEY_OMICSDI_EXPORT_DIRTY
        capturedValue
    }

    void "isExportDirty reflects whatever redisService.exists reports for the dirty-flag key"() {
        given:
        RedisService.metaClass.static.exists = { String key -> key == Redis.REDIS_KEY_OMICSDI_EXPORT_DIRTY }

        expect:
        service.isExportDirty()
    }

    void "isExportDirty is false when the dirty-flag key is absent"() {
        given:
        RedisService.metaClass.static.exists = { String key -> false }

        expect:
        !service.isExportDirty()
    }

    void "saveOmicsdiExportSettings rewrites a jdbc:mariadb datasource URL to jdbc:mysql for the indexer jar"() {
        // The standalone indexer jar bundles only MySQL Connector/J; a jdbc:mariadb: URL makes it
        // abort at startup with "No suitable driver" and no XML is produced.
        given:
        stubExportSettingsConfig("jdbc:mariadb://localhost:3306/biomodels_prod?useUnicode=true&characterEncoding=UTF-8")

        when:
        File settings = service.saveOmicsdiExportSettings([:])

        then:
        new JsonSlurper().parse(settings).database.url ==
                "jdbc:mysql://localhost:3306/biomodels_prod?useUnicode=true&characterEncoding=UTF-8"
    }

    @Unroll
    void "saveOmicsdiExportSettings passes a #scheme datasource URL through to the indexer jar unchanged"() {
        given:
        stubExportSettingsConfig(url)

        when:
        File settings = service.saveOmicsdiExportSettings([:])

        then:
        new JsonSlurper().parse(settings).database.url == url

        where:
        scheme       | url
        "jdbc:mysql" | "jdbc:mysql://localhost:3306/biomodels_prod?useUnicode=true"
        "postgresql" | "jdbc:postgresql://localhost:5432/biomodels_prod"
        "h2"         | "jdbc:h2:mem:testDb"
    }

    void "archiveExportedXmlToGit is a no-op and leaves the dirty flag untouched when git archiving is not configured"() {
        given:
        grailsApplication.config.jummp.omicsdi.git.enabled = false
        boolean deleteCalled = false
        RedisService.metaClass.static.doRedisDel = { String key -> deleteCalled = true }

        expect:
        !service.archiveExportedXmlToGit()
        !deleteCalled
    }

    void "archiveExportedXmlToGit copies exported XML, commits, pushes it, and clears the dirty flag"() {
        given: "a bare repo standing in for the real GitHub remote"
        File remoteDir = new File(tempDir, "remote.git")
        runGit(tempDir, "init", "--bare", "--initial-branch=metadata", remoteDir.canonicalPath)

        and: "a local clone of it, standing in for the host's existing checkout, with one commit so the branch exists"
        File repoDir = new File(tempDir, "repo")
        runGit(tempDir, "clone", remoteDir.canonicalPath, repoDir.canonicalPath)
        runGit(repoDir, "-c", "user.name=Test", "-c", "user.email=test@example.com",
                "commit", "--allow-empty", "-m", "init")
        runGit(repoDir, "push", "origin", "HEAD:metadata")

        and: "an export folder with a freshly generated XML file"
        File exportFolder = new File(tempDir, "export")
        exportFolder.mkdirs()
        new File(exportFolder, "entries.xml").text = "<database><entries/></database>"

        and:
        configureGit(repoDir, exportFolder)
        boolean deleteCalled = false
        RedisService.metaClass.static.doRedisDel = { String key -> deleteCalled = true }

        when:
        boolean result = service.archiveExportedXmlToGit()

        then:
        result
        deleteCalled
        new File(repoDir, "omicsdi/entries.xml").text == "<database><entries/></database>"
        runGit(remoteDir, "log", "metadata", "--oneline").contains("Automated OmicsDI export")
    }

    void "archiveExportedXmlToGit clears the dirty flag without creating an empty commit when nothing changed"() {
        given: "a repo that already has today's XML committed and pushed"
        File remoteDir = new File(tempDir, "remote.git")
        runGit(tempDir, "init", "--bare", "--initial-branch=metadata", remoteDir.canonicalPath)
        File repoDir = new File(tempDir, "repo")
        runGit(tempDir, "clone", remoteDir.canonicalPath, repoDir.canonicalPath)
        File subdir = new File(repoDir, "omicsdi")
        subdir.mkdirs()
        new File(subdir, "entries.xml").text = "<database><entries/></database>"
        runGit(repoDir, "add", "-A")
        runGit(repoDir, "-c", "user.name=Test", "-c", "user.email=test@example.com",
                "commit", "-m", "pre-existing export")
        runGit(repoDir, "push", "origin", "HEAD:metadata")
        String beforeLog = runGit(remoteDir, "log", "metadata", "--oneline")

        and: "the export folder produces byte-identical XML"
        File exportFolder = new File(tempDir, "export")
        exportFolder.mkdirs()
        new File(exportFolder, "entries.xml").text = "<database><entries/></database>"

        and:
        configureGit(repoDir, exportFolder)
        boolean deleteCalled = false
        RedisService.metaClass.static.doRedisDel = { String key -> deleteCalled = true }

        when:
        boolean result = service.archiveExportedXmlToGit()

        then:
        result
        deleteCalled
        runGit(remoteDir, "log", "metadata", "--oneline") == beforeLog
    }

    void "archiveExportedXmlToGit leaves the dirty flag set when the git repo is unusable"() {
        given: "a repoPath that is not actually a git repository"
        File notARepo = new File(tempDir, "not-a-repo")
        notARepo.mkdirs()
        File exportFolder = new File(tempDir, "export")
        exportFolder.mkdirs()
        new File(exportFolder, "entries.xml").text = "<database/>"

        and:
        configureGit(notARepo, exportFolder)
        boolean deleteCalled = false
        RedisService.metaClass.static.doRedisDel = { String key -> deleteCalled = true }

        when:
        boolean result = service.archiveExportedXmlToGit()

        then:
        !result
        !deleteCalled
    }

    void "archiveExportedXmlToGit refuses a relative repoPath instead of falling back to the JVM's working directory"() {
        // Regression test: a .properties-style config file (unlike Groovy ConfigSlurper) does not
        // strip quotes around a value, so `repoPath="/some/path"` there previously produced the
        // literal string `"/some/path"` - not absolute - which made new File(...) resolve relative
        // to cwd (this app's own checkout) and silently git-commit/push into the wrong repo.
        given: "an export folder with a freshly generated XML file"
        File exportFolder = new File(tempDir, "export")
        exportFolder.mkdirs()
        new File(exportFolder, "entries.xml").text = "<database/>"

        and: "repoPath is relative, as it would be if wrapped in stray quote characters"
        configureGit(new File(tempDir, "repo"), exportFolder)
        grailsApplication.config.jummp.omicsdi.git.repoPath = "some/relative/path"
        boolean deleteCalled = false
        RedisService.metaClass.static.doRedisDel = { String key -> deleteCalled = true }

        when:
        boolean result = service.archiveExportedXmlToGit()

        then: "it refuses outright rather than mkdir'ing/committing anywhere"
        !result
        !deleteCalled
        !new File("some").exists()
    }

    void "archiveExportedXmlToGit strips stray quote characters from configured git properties"() {
        // Regression test for the same misconfiguration as above, but where repoPath still
        // happens to resolve to a real absolute path once the surrounding quotes are stripped -
        // e.g. `repoPath="/abs/path"` in a .properties file.
        given: "a bare repo standing in for the real GitHub remote"
        File remoteDir = new File(tempDir, "remote.git")
        runGit(tempDir, "init", "--bare", "--initial-branch=metadata", remoteDir.canonicalPath)

        and: "a local clone of it, with one commit so the branch exists"
        File repoDir = new File(tempDir, "repo")
        runGit(tempDir, "clone", remoteDir.canonicalPath, repoDir.canonicalPath)
        runGit(repoDir, "-c", "user.name=Test", "-c", "user.email=test@example.com",
                "commit", "--allow-empty", "-m", "init")
        runGit(repoDir, "push", "origin", "HEAD:metadata")

        and: "an export folder with a freshly generated XML file"
        File exportFolder = new File(tempDir, "export")
        exportFolder.mkdirs()
        new File(exportFolder, "entries.xml").text = "<database><entries/></database>"

        and: "every configured git property is wrapped in stray quote characters"
        configureGit(repoDir, exportFolder)
        grailsApplication.config.jummp.omicsdi.git.repoPath = "\"${repoDir.canonicalPath}\"".toString()
        grailsApplication.config.jummp.omicsdi.git.subdir = '"omicsdi"'
        grailsApplication.config.jummp.omicsdi.git.remote = "'origin'"
        grailsApplication.config.jummp.omicsdi.git.branch = '"metadata"'
        boolean deleteCalled = false
        RedisService.metaClass.static.doRedisDel = { String key -> deleteCalled = true }

        when:
        boolean result = service.archiveExportedXmlToGit()

        then:
        result
        deleteCalled
        new File(repoDir, "omicsdi/entries.xml").text == "<database><entries/></database>"
        runGit(remoteDir, "log", "metadata", "--oneline").contains("Automated OmicsDI export")
    }

    private void stubExportSettingsConfig(String datasourceUrl) {
        grailsApplication.config.dataSource = [url: datasourceUrl, username: "root", password: "secret"]
        grailsApplication.config.jummp.search.strategy = "omicsdi"
        grailsApplication.config.jummp.search.exportFolder = tempDir.canonicalPath
        grailsApplication.config.jummp.metadata.officialDatabaseName = "BioModels"
        grailsApplication.config.grails.serverURL = "http://localhost:8080/jummp"
        service.configurationService = new Object() {
            String getConfigFilePath() { "/etc/jummp/jummp-config.properties" }
        }
    }

    private void configureGit(File repoDir, File exportFolder) {
        grailsApplication.config.jummp.omicsdi.git.enabled = true
        grailsApplication.config.jummp.omicsdi.git.repoPath = repoDir.canonicalPath
        grailsApplication.config.jummp.omicsdi.git.subdir = "omicsdi"
        grailsApplication.config.jummp.omicsdi.git.remote = "origin"
        grailsApplication.config.jummp.omicsdi.git.branch = "metadata"
        grailsApplication.config.jummp.omicsdi.git.commitUserName = "Test Bot"
        grailsApplication.config.jummp.omicsdi.git.commitUserEmail = "test@example.com"
        grailsApplication.config.jummp.search.exportFolder = exportFolder.canonicalPath
    }

    private static String runGit(File dir, String... args) {
        List<String> command = ["git"] + (args as List)
        Process process = new ProcessBuilder(command).directory(dir).redirectErrorStream(true).start()
        String output = process.inputStream.getText("UTF-8")
        process.waitFor()
        if (process.exitValue() != 0) {
            throw new IllegalStateException("git ${args.join(' ')} in ${dir} failed:\n${output}")
        }
        output
    }
}
