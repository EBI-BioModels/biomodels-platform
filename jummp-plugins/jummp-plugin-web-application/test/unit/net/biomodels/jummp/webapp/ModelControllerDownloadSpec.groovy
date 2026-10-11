package net.biomodels.jummp.webapp

import grails.test.mixin.TestFor
import net.biomodels.jummp.core.constants.BioModels
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.core.util.JummpHttpService
import net.biomodels.jummp.utils.redis.RedisService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Covers the fallbacks of the private serveModel*** helpers behind ModelController.download.
 */
@TestFor(ModelController)
class ModelControllerDownloadSpec extends Specification {
    static final String FTP = "http://ftp.example.org/repository"
    static final String OMEX_PATH = "MODEL2401190005${File.separator}2${File.separator}MODEL2401190005.2.omex"
    static final String CONTENT = "<sbml>0123456789</sbml>"

    // CommonController.setConfiguration() looks these up when the controller is created
    static doWithSpring = {
        redisService(RedisService)
    }
    static doWithConfig = { c ->
        c.jummp.branding.style = "biomodels"
        c.jummp.context.help.root = "http://localhost/help"
        c.grails.serverURL = "http://localhost:8080"
        c.jummp.model.ftp.location = ""
    }

    File tmpDir
    File modelFile
    RFTC rf
    RTC revision
    String savedFtp
    String savedDeployTarget
    long savedMaxFileSize

    def setup() {
        savedFtp = ModelController.PUBLIC_FTP_REPO
        savedDeployTarget = ModelController.deployTarget
        config.jummp.model.download.server = 'http://download.example.org'
        savedMaxFileSize = BioModels.MAX_FILE_SIZE
        tmpDir = File.createTempDir("model-controller-download", "")
        modelFile = new File(tmpDir, "model.xml")
        modelFile.text = CONTENT
        rf = new RFTC(id: 1L, path: modelFile.absolutePath, mimeType: "application/xml",
            size: modelFile.length())
        revision = new RTC(state: ModelState.PUBLISHED, revisionNumber: 2,
            model: new MTC(submissionId: "MODEL2401190005", publicationId: "BIOMD0000000001"))
        controller.modelDelegateService = new Expando(
            getRevisionsState: { String id -> [vcsId: "BIOMD0000000001"] })
        config.jummp.web.file.preview = 5
    }

    def cleanup() {
        ModelController.PUBLIC_FTP_REPO = savedFtp
        ModelController.deployTarget = savedDeployTarget
        BioModels.MAX_FILE_SIZE = savedMaxFileSize
        tmpDir?.deleteDir()
    }

    void "published file without an FTP repo is served from disk, not as an OMEX archive"() {
        given:
        ModelController.PUBLIC_FTP_REPO = ""

        when:
        controller.serveModelAsFileForPublished(revision, rf, "model.xml", response, false)

        then:
        response.redirectedUrl == null
        response.contentAsString == CONTENT
        response.getHeader("Content-Disposition") == 'attachment;filename="model.xml"'
    }

    void "published file is served from disk when the FTP mirror is unreachable"() {
        given:
        ModelController.PUBLIC_FTP_REPO = FTP
        GroovySpy(JummpHttpService, global: true)
        JummpHttpService.isReachable(_) >> false

        when:
        controller.serveModelAsFileForPublished(revision, rf, "model.xml", response, false)

        then:
        response.redirectedUrl == null
        response.contentAsString == CONTENT
    }

    void "published file is redirected to the FTP mirror when reachable"() {
        given:
        ModelController.PUBLIC_FTP_REPO = FTP
        GroovySpy(JummpHttpService, global: true)
        JummpHttpService.isReachable(_) >> true

        when:
        controller.serveModelAsFileForPublished(revision, rf, "model.xml", response, false)

        then:
        response.redirectedUrl ==
            "${FTP}/BIOMD0000000001/MODEL2401190005${File.separator}2/model.xml".toString()
        response.contentAsString == ""
    }

    void "previews of published files never touch the FTP mirror"() {
        given:
        ModelController.PUBLIC_FTP_REPO = FTP
        GroovySpy(JummpHttpService, global: true)

        when:
        controller.serveModelAsFile(revision, rf, "model.xml", response, true, true)

        then:
        0 * JummpHttpService.isReachable(_)
        response.contentAsString == CONTENT.substring(0, 5)
        response.getHeader("Content-Disposition") == 'inline;filename="model.xml"'
    }

    void "a preview larger than the file serves the whole file"() {
        given:
        config.jummp.web.file.preview = 1000

        when:
        controller.serveModelAsFileInstantly(rf, response, false, true)

        then:
        response.contentAsString == CONTENT
    }

    void "files over the size limit are replaced by the BIG_FILE marker without failing"() {
        given:
        BioModels.MAX_FILE_SIZE = 4

        when:
        controller.serveModelAsFileInstantly(rf, response, false)

        then:
        notThrown(Exception)
        response.contentAsString == "BIG_FILE"
    }

    void "private large files answer 413 instead of being served"() {
        given:
        rf.size = BioModels.MAX_FILE_SIZE
        revision.state = ModelState.UNPUBLISHED

        when:
        controller.serveModelAsFile(revision, rf, "model.xml", response, false)

        then:
        response.forwardedUrl == "/errors/error413"
    }

    void "private small files are served from disk"() {
        given:
        revision.state = ModelState.UNPUBLISHED

        when:
        controller.serveModelAsFile(revision, rf, "model.xml", response, false)

        then:
        response.contentAsString == CONTENT
    }

    // --- OMEX of a published revision: FTP mirror -> download service -> on the fly -------------------------

    // the OMEX is already in the model cache, so the download-service tier redirects to its get-files URL
    private String seedOmexInCache() {
        File cache = new File(tmpDir, "cache")
        File omex = new File(cache, OMEX_PATH)
        omex.parentFile.mkdirs()
        omex.text = "omex"
        config.jummp.model.cache.dir = cache.absolutePath
        "http://download.example.org/get-files/${OMEX_PATH}".toString()
    }

    private void omexCreatedOnTheFly() {
        File omex = new File(tmpDir, "MODEL2401190005.2.omex")
        omex.text = "generated-omex"
        controller.omexService = new Expando(createCombineArchive: { files, id, rev, flag -> omex.absolutePath })
    }

    private void mirrorAndServiceReachable(boolean mirror, boolean service) {
        GroovySpy(JummpHttpService, global: true)
        JummpHttpService.isReachable({ it.startsWith(FTP) }) >> mirror
        JummpHttpService.isReachable("http://download.example.org/healthcheck") >> service
    }

    void "published OMEX is redirected to the FTP mirror when it is reachable"() {
        given:
        ModelController.deployTarget = "prod"
        ModelController.PUBLIC_FTP_REPO = FTP
        mirrorAndServiceReachable(true, true)

        when:
        controller.serveModelAsCombineArchiveForPublished(revision, [rf], response)

        then:
        response.redirectedUrl.startsWith(FTP)
        response.redirectedUrl.endsWith("MODEL2401190005.2.omex")
    }

    @Unroll
    void "published OMEX goes to the download service when the mirror is #why"() {
        given:
        ModelController.deployTarget = "prod"
        ModelController.PUBLIC_FTP_REPO = ftp
        mirrorAndServiceReachable(false, true)
        String serviceUrl = seedOmexInCache()

        when:
        controller.serveModelAsCombineArchiveForPublished(revision, [rf], response)

        then:
        response.redirectedUrl == serviceUrl

        where:
        why              | ftp
        "unreachable"    | FTP
        "not configured" | ""
    }

    void "the download service is probed on /healthcheck even when its URL has a trailing slash"() {
        given:
        ModelController.deployTarget = "prod"
        ModelController.PUBLIC_FTP_REPO = FTP
        mirrorAndServiceReachable(false, true)
        config.jummp.model.download.server = "http://download.example.org//"
        // no doubled slash in the link handed to the user either
        String serviceUrl = seedOmexInCache()

        when:
        controller.serveModelAsCombineArchiveForPublished(revision, [rf], response)

        then:
        response.redirectedUrl == serviceUrl
    }

    void "an unset download service is skipped without probing it"() {
        given:
        ModelController.deployTarget = "prod"
        ModelController.PUBLIC_FTP_REPO = FTP
        mirrorAndServiceReachable(false, true)
        config.jummp.model.download.server = new ConfigObject()
        omexCreatedOnTheFly()

        when:
        controller.serveModelAsCombineArchiveForPublished(revision, [rf], response)

        then:
        response.contentAsString == "generated-omex"
    }

    void "published small OMEX is created on the fly when neither mirror nor download service is available"() {
        given:
        ModelController.deployTarget = "prod"
        ModelController.PUBLIC_FTP_REPO = FTP
        mirrorAndServiceReachable(false, false)
        omexCreatedOnTheFly()

        when:
        controller.serveModelAsCombineArchiveForPublished(revision, [rf], response)

        then:
        response.redirectedUrl == null
        response.contentType == "application/zip"
        response.contentAsString == "generated-omex"
    }

    void "published large OMEX answers 413 when neither mirror nor download service is available"() {
        given:
        ModelController.deployTarget = "prod"
        ModelController.PUBLIC_FTP_REPO = FTP
        mirrorAndServiceReachable(false, false)
        rf.size = BioModels.MAX_FILE_SIZE

        when:
        controller.serveModelAsCombineArchiveForPublished(revision, [rf], response)

        then:
        response.contentAsString == ""
        response.forwardedUrl == "/errors/error413"
    }

    void "a local deployment always uses the download service"() {
        given:
        ModelController.deployTarget = "local"
        ModelController.PUBLIC_FTP_REPO = FTP
        String serviceUrl = seedOmexInCache()

        when:
        controller.serveModelAsCombineArchiveForPublished(revision, [rf], response)

        then:
        response.redirectedUrl == serviceUrl
    }

    void "published file falls back to disk, not the download service, when the mirror is unreachable"() {
        given:
        ModelController.PUBLIC_FTP_REPO = FTP
        mirrorAndServiceReachable(false, true)

        when:
        controller.serveModelAsFileForPublished(revision, rf, "model.xml", response, false)

        then:
        response.redirectedUrl == null
        response.contentAsString == CONTENT
    }

    @Unroll
    void "download of unknown file '#name' answers 400, not a NullPointerException"() {
        given:
        params.id = "MODEL2401190005"
        params.filename = name
        controller.modelDelegateService = new Expando(
            getRevisionFromParams: { String id, String rev -> revision },
            retrieveModelFiles: { RTC r -> [rf] })
        // the JSON error body proves the "unknown file" branch ran, not the generic catch block
        request.format = "json"
        response.format = "json"

        when:
        controller.download()

        then:
        response.status == 400
        response.contentAsString.contains("Invalid file name")

        where:
        name << ["missing.xml", "another.sbml"]
    }
}
