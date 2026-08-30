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

    void "saveOmicsdiExportSettings writes a miriamExportFile inside the export folder"() {
        // The indexer's RequestContext constructor throws when this key is absent, which silently
        // aborted every app-triggered export.
        given:
        stubExportSettingsConfig("jdbc:mysql://localhost:3306/biomodels_prod")

        when:
        File settings = service.saveOmicsdiExportSettings([:])

        then:
        new JsonSlurper().parse(settings).miriamExportFile ==
                new File(tempDir.canonicalPath, "miriam.xml").path
    }

    @Unroll
    void "saveOmicsdiExportSettings coerces numberEntriesOnEachFile #raw to #expected when allowMultipleFiles is set"() {
        given:
        stubExportSettingsConfig("jdbc:mysql://localhost:3306/biomodels_prod")

        when:
        File settings = service.saveOmicsdiExportSettings(
                [allowMultipleFiles: true, numberEntriesOnEachFile: raw, tagsExcluded: []])

        then:
        new JsonSlurper().parse(settings).options.numberEntriesOnEachFile == expected

        where:
        raw          | expected
        "undefined"  | 1000
        ""           | 1000
        0            | 1000
        -5           | 1000
        250          | 250
        "500"        | 500
    }

    void "saveOmicsdiExportSettings leaves numberEntriesOnEachFile alone when a single file is requested"() {
        given:
        stubExportSettingsConfig("jdbc:mysql://localhost:3306/biomodels_prod")

        when:
        File settings = service.saveOmicsdiExportSettings(
                [allowMultipleFiles: false, numberEntriesOnEachFile: "undefined", tagsExcluded: []])

        then:
        new JsonSlurper().parse(settings).options.numberEntriesOnEachFile == "undefined"
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

    void "archiveExportedXmlToGit bundles the latest run into biomodels.zip, commits it under the run's date, pushes, clears the dirty flag"() {
        given:
        File repoDir = freshArchiveClone()
        File exportFolder = new File(tempDir, "export"); exportFolder.mkdirs()
        new File(exportFolder, "OmicsDIEntries-20260830-123537-1.xml").text = "<part 1/>"
        new File(exportFolder, "OmicsDIEntries-20260830-123537-2.xml").text = "<part 2/>"
        new File(exportFolder, "miriam.xml").text = "<registry/>"
        configureGit(repoDir, exportFolder)
        boolean deleteCalled = false
        RedisService.metaClass.static.doRedisDel = { String key -> deleteCalled = true }

        when:
        boolean result = service.archiveExportedXmlToGit()

        then:
        result
        deleteCalled
        File zip = new File(repoDir, "omicsdi/biomodels.zip")
        zipEntryNames(zip) == ["OmicsDIEntries-20260830-123537-1.xml", "OmicsDIEntries-20260830-123537-2.xml"]
        zipEntryText(zip, "OmicsDIEntries-20260830-123537-1.xml") == "<part 1/>"
        !new File(repoDir, "omicsdi/miriam.xml").exists()
        runGit(repoDir, "log", "metadata", "--oneline").contains("Add BioModels metadata exported on 2026-08-30")
    }

    void "archiveExportedXmlToGit rotates the previous biomodels.zip to BioModels-metadata-<its date>.zip"() {
        given:
        File repoDir = freshArchiveClone()
        File subdir = new File(repoDir, "omicsdi"); subdir.mkdirs()
        makeZip(new File(subdir, "biomodels.zip"), ["OmicsDIEntries-20260827-100000-1.xml": "<previous run/>"])
        runGit(repoDir, "add", "-A")
        runGit(repoDir, "-c", "user.name=T", "-c", "user.email=t@e.com", "commit", "-m", "prior export")
        runGit(repoDir, "push", "origin", "HEAD:metadata")

        File exportFolder = new File(tempDir, "export"); exportFolder.mkdirs()
        new File(exportFolder, "OmicsDIEntries-20260830-123537-1.xml").text = "<new run/>"
        configureGit(repoDir, exportFolder)
        RedisService.metaClass.static.doRedisDel = { String key -> }

        when:
        boolean result = service.archiveExportedXmlToGit()

        then: "the old export is preserved under its own date and biomodels.zip carries the new one"
        result
        File rotated = new File(subdir, "BioModels-metadata-20260827.zip")
        zipEntryText(rotated, "OmicsDIEntries-20260827-100000-1.xml") == "<previous run/>"
        zipEntryNames(new File(subdir, "biomodels.zip")) == ["OmicsDIEntries-20260830-123537-1.xml"]
    }

    void "archiveExportedXmlToGit makes no commit when biomodels.zip already holds the latest run"() {
        given:
        File repoDir = freshArchiveClone()
        File subdir = new File(repoDir, "omicsdi"); subdir.mkdirs()
        makeZip(new File(subdir, "biomodels.zip"), ["OmicsDIEntries-20260830-123537-1.xml": "<x/>"])
        runGit(repoDir, "add", "-A")
        runGit(repoDir, "-c", "user.name=T", "-c", "user.email=t@e.com", "commit", "-m", "already archived")
        runGit(repoDir, "push", "origin", "HEAD:metadata")
        String beforeLog = runGit(repoDir, "log", "metadata", "--oneline")

        File exportFolder = new File(tempDir, "export"); exportFolder.mkdirs()
        new File(exportFolder, "OmicsDIEntries-20260830-123537-1.xml").text = "<x/>"
        configureGit(repoDir, exportFolder)
        boolean deleteCalled = false
        RedisService.metaClass.static.doRedisDel = { String key -> deleteCalled = true }

        when:
        boolean result = service.archiveExportedXmlToGit()

        then:
        result
        deleteCalled
        !new File(subdir, "BioModels-metadata-20260830.zip").exists()
        runGit(repoDir, "log", "metadata", "--oneline") == beforeLog
    }

    void "archiveExportedXmlToGit sweeps up loose OmicsDIEntries XML from earlier breakage but keeps miriam.xml"() {
        given:
        File repoDir = freshArchiveClone()
        File subdir = new File(repoDir, "omicsdi"); subdir.mkdirs()
        new File(subdir, "OmicsDIEntries-20260830-074824-1.xml").text = "<loose stale/>"
        new File(subdir, "miriam.xml").text = "<registry/>"
        runGit(repoDir, "add", "-A")
        runGit(repoDir, "-c", "user.name=T", "-c", "user.email=t@e.com", "commit", "-m", "breakage")
        runGit(repoDir, "push", "origin", "HEAD:metadata")

        File exportFolder = new File(tempDir, "export"); exportFolder.mkdirs()
        new File(exportFolder, "OmicsDIEntries-20260830-123537-1.xml").text = "<fresh/>"
        configureGit(repoDir, exportFolder)
        RedisService.metaClass.static.doRedisDel = { String key -> }

        when:
        boolean result = service.archiveExportedXmlToGit()

        then:
        result
        !new File(subdir, "OmicsDIEntries-20260830-074824-1.xml").exists()
        new File(subdir, "miriam.xml").text == "<registry/>"
        new File(subdir, "biomodels.zip").isFile()
    }

    void "archiveExportedXmlToGit leaves the archive alone when the export folder has no OmicsDIEntries files"() {
        given:
        File repoDir = freshArchiveClone()
        File subdir = new File(repoDir, "omicsdi"); subdir.mkdirs()
        makeZip(new File(subdir, "biomodels.zip"), ["OmicsDIEntries-20260830-123537-1.xml": "<kept/>"])
        runGit(repoDir, "add", "-A")
        runGit(repoDir, "-c", "user.name=T", "-c", "user.email=t@e.com", "commit", "-m", "prior")
        runGit(repoDir, "push", "origin", "HEAD:metadata")
        String beforeLog = runGit(repoDir, "log", "metadata", "--oneline")

        File exportFolder = new File(tempDir, "export"); exportFolder.mkdirs()
        new File(exportFolder, "miriam.xml").text = "<registry/>"
        configureGit(repoDir, exportFolder)
        boolean deleteCalled = false
        RedisService.metaClass.static.doRedisDel = { String key -> deleteCalled = true }

        when:
        boolean result = service.archiveExportedXmlToGit()

        then:
        result
        deleteCalled
        zipEntryText(new File(subdir, "biomodels.zip"), "OmicsDIEntries-20260830-123537-1.xml") == "<kept/>"
        runGit(repoDir, "log", "metadata", "--oneline") == beforeLog
    }

    void "archiveExportedXmlToGit leaves the dirty flag set when the git repo is unusable"() {
        given:
        File notARepo = new File(tempDir, "not-a-repo"); notARepo.mkdirs()
        File exportFolder = new File(tempDir, "export"); exportFolder.mkdirs()
        new File(exportFolder, "OmicsDIEntries-20260830-123537-1.xml").text = "<x/>"
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
        given:
        File exportFolder = new File(tempDir, "export"); exportFolder.mkdirs()
        new File(exportFolder, "OmicsDIEntries-20260830-123537-1.xml").text = "<x/>"
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
        // Regression test for the same misconfiguration as above, but where repoPath still resolves
        // to a real absolute path once the surrounding quotes are stripped.
        given:
        File repoDir = freshArchiveClone()
        File exportFolder = new File(tempDir, "export"); exportFolder.mkdirs()
        new File(exportFolder, "OmicsDIEntries-20260830-123537-1.xml").text = "<x/>"
        configureGit(repoDir, exportFolder)
        grailsApplication.config.jummp.omicsdi.git.repoPath = "\"${repoDir.canonicalPath}\"".toString()
        grailsApplication.config.jummp.omicsdi.git.subdir = '"omicsdi"'
        grailsApplication.config.jummp.omicsdi.git.remote = "'origin'"
        grailsApplication.config.jummp.omicsdi.git.branch = '"metadata"'
        RedisService.metaClass.static.doRedisDel = { String key -> }

        when:
        boolean result = service.archiveExportedXmlToGit()

        then:
        result
        zipEntryNames(new File(repoDir, "omicsdi/biomodels.zip")) == ["OmicsDIEntries-20260830-123537-1.xml"]
        runGit(repoDir, "log", "metadata", "--oneline").contains("Add BioModels metadata exported on 2026-08-30")
    }

    void "archiveExportedXmlToGit treats an empty subdir as the repo root instead of aborting on an empty git pathspec"() {
        // `jummp.omicsdi.git.subdir=` in the .properties config reaches the service as "";
        // `git add -- ""` aborts with "empty string is not a valid pathspec" (git >= 2.16), so it
        // is normalised to "." and the archive lands at the repo root.
        given:
        File repoDir = freshArchiveClone()
        File exportFolder = new File(tempDir, "export"); exportFolder.mkdirs()
        new File(exportFolder, "OmicsDIEntries-20260830-123537-1.xml").text = "<x/>"
        configureGit(repoDir, exportFolder)
        grailsApplication.config.jummp.omicsdi.git.subdir = ""
        RedisService.metaClass.static.doRedisDel = { String key -> }

        when:
        boolean result = service.archiveExportedXmlToGit()

        then:
        result
        new File(repoDir, "biomodels.zip").isFile()
        runGit(repoDir, "log", "metadata", "--oneline").contains("Add BioModels metadata exported on 2026-08-30")
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

    /** Bare remote on branch `metadata` + a local clone with one commit, ready to archive into. */
    private File freshArchiveClone() {
        File remoteDir = new File(tempDir, "remote.git")
        runGit(tempDir, "init", "--bare", "--initial-branch=metadata", remoteDir.canonicalPath)
        File repoDir = new File(tempDir, "repo")
        runGit(tempDir, "clone", remoteDir.canonicalPath, repoDir.canonicalPath)
        runGit(repoDir, "-c", "user.name=Test", "-c", "user.email=test@example.com",
                "commit", "--allow-empty", "-m", "init")
        runGit(repoDir, "push", "origin", "HEAD:metadata")
        repoDir
    }

    private static void makeZip(File target, Map<String, String> entries) {
        target.parentFile?.mkdirs()
        new java.util.zip.ZipOutputStream(new FileOutputStream(target)).withCloseable { zos ->
            entries.each { String name, String content ->
                zos.putNextEntry(new java.util.zip.ZipEntry(name))
                zos.write(content.getBytes("UTF-8"))
                zos.closeEntry()
            }
        }
    }

    private static List<String> zipEntryNames(File zip) {
        new java.util.zip.ZipFile(zip).withCloseable { zf -> zf.entries().collect { it.name }.sort() }
    }

    private static String zipEntryText(File zip, String name) {
        new java.util.zip.ZipFile(zip).withCloseable { zf ->
            zf.getInputStream(zf.getEntry(name)).getText("UTF-8")
        }
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
