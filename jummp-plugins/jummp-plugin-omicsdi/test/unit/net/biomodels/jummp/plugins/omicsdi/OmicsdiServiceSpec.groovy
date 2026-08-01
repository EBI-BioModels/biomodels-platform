package net.biomodels.jummp.plugins.omicsdi

import grails.test.mixin.TestFor
import net.biomodels.jummp.core.constants.Redis
import net.biomodels.jummp.utils.redis.RedisService
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions.
 *
 * RedisService's actual read/write methods are `static`, backed by a live Jedis pool, so they
 * can't be intercepted with a normal Spock Mock(). Instead - following the same pattern already
 * used in ModelIdentifierGeneratorIntegrationSpec - a real RedisService instance is assigned and
 * individual methods are overridden per-test via closure reassignment on that instance.
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
    }

    void "markExportDirty writes a non-empty value to the omicsdi dirty-flag redis key"() {
        given:
        String capturedKey
        String capturedValue
        service.redisService.doRedisSet = { String key, String value ->
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
        service.redisService.exists = { String key -> key == Redis.REDIS_KEY_OMICSDI_EXPORT_DIRTY }

        expect:
        service.isExportDirty()
    }

    void "isExportDirty is false when the dirty-flag key is absent"() {
        given:
        service.redisService.exists = { String key -> false }

        expect:
        !service.isExportDirty()
    }

    void "archiveExportedXmlToGit is a no-op and leaves the dirty flag untouched when git archiving is not configured"() {
        given:
        grailsApplication.config.jummp.omicsdi.git.enabled = false
        boolean deleteCalled = false
        service.redisService.doRedisDel = { String key -> deleteCalled = true }

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
        service.redisService.doRedisDel = { String key -> deleteCalled = true }

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
        service.redisService.doRedisDel = { String key -> deleteCalled = true }

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
        service.redisService.doRedisDel = { String key -> deleteCalled = true }

        when:
        boolean result = service.archiveExportedXmlToGit()

        then:
        !result
        !deleteCalled
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
