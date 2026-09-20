package net.biomodels.jummp.webapp

import grails.test.mixin.TestFor
import net.biomodels.jummp.core.model.ModelFormatTransportCommand as MFTC
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.utils.redis.RedisService
import spock.lang.Specification

/**
 * Covers JBM-796 and JBM-798: what SubmissionController does with the files of a submission and with a submission that
 * SubmissionService refuses, in the API's create action and in the last validation of the web wizard.
 *
 * SubmissionControllerSpec, which is in the web application plugin, is not run from the root of the project (the
 * inline plugins' test/unit directories are not scanned), so this spec is here, where it runs.
 */
@TestFor(SubmissionController)
class SubmissionControllerValidationSpec extends Specification {
    private File dir

    // what CommonController and SubmissionController read when the controller is created, before setup() runs
    static doWithSpring = {
        redisService(RedisService)
    }

    static doWithConfig = { config ->
        config.jummp.branding.style = "biomodels"
        config.grails.serverURL = "http://localhost:8080/biomodels"
        config.jummp.context.help.root = "http://localhost:8080/biomodels/help"
        config.jummp.model.ftp.location = "ftp://localhost/biomodels"
        config.jummp.vcs.exchangeDirectory = System.getProperty("java.io.tmpdir")
    }

    void setup() {
        dir = File.createTempDir("submission-controller", "")
        // the parameters of the upload step are html-encoded
        mockCodec(org.codehaus.groovy.grails.plugins.codecs.HTMLCodec)
    }

    void cleanup() {
        dir.deleteDir()
    }

    private RFTC fileWith(String name, String text) {
        File file = new File(dir, name)
        if (text != null) {
            file.text = text
        }
        new RFTC(path: file.path, mainFile: true)
    }

    // ------------------------------------------------------------------------ doValidateUploadedFiles

    void "the files are valid when each one exists and has content"() {
        when:
        boolean valid = controller.doValidateUploadedFiles([repository_files: [fileWith("a.xml", "<a/>"), fileWith("b.txt", "b")]])

        then:
        valid
        controller.validationMessages[0] == ""
    }

    void "a missing file makes the files invalid and is named"() {
        when:
        boolean valid = controller.doValidateUploadedFiles([repository_files: [fileWith("a.xml", "<a/>"), fileWith("never.txt", null)]])

        then:
        !valid
        controller.validationMessages[0] == "never.txt: Not found or not exist or empty.\n"
    }

    void "an empty file makes the files invalid and is named"() {
        when:
        boolean valid = controller.doValidateUploadedFiles([repository_files: [fileWith("empty.txt", ""), fileWith("a.xml", "<a/>")]])

        then:
        !valid
        controller.validationMessages[0] == "empty.txt: Not found or not exist or empty.\n"
    }

    // ------------------------------------------------------------------------ makeSubmission

    /** What SubmissionService.buildFromJSONFile leaves in the working memory once it accepts a submission. */
    private Closure builds(List<RFTC> files) {
        { String metadata, Map working ->
            MTC model = new MTC(name: "A model")
            working.putAll(repository_files: files, ModelTC: model, modelling_approach: "Other",
                RevisionTC: new RTC(model: model, format: new MFTC(identifier: "UNKNOWN", name: "Unknown")))
        }
    }

    private Map create(Map submissionService) {
        controller.springSecurityService = [currentUser: [username: "testuser", email: "test@test.com", person: [userRealName: "Test"]]]
        controller.submissionService = submissionService
        request.method = "POST"
        request.addHeader("SubmissionFolder", "a-submission-folder")
        request.content = '{"name": "A model"}'.bytes
        params.format = "json"
        controller.create()
        response.json as Map
    }

    void "a submission the service refuses is answered with a 400 and the reason it recorded"() {
        given:
        Map service = [buildFromJSONFile: { String metadata, Map working ->
            working.cause = "The model identifier is missing."
            throw new IllegalAccessException("The model identifier is missing. The update process has been terminated!")
        }]

        when:
        Map answer = create(service)

        then:
        answer.status == 400
        answer.message == "The model identifier is missing."
    }

    void "a submission the service refuses without a reason is answered with the message of the exception"() {
        given:
        Map service = [buildFromJSONFile: { String metadata, Map working ->
            throw new FileNotFoundException("Cannot find the model files.")
        }]

        when:
        Map answer = create(service)

        then:
        answer.status == 400
        answer.message == "Cannot find the model files."
    }

    void "a submission whose file was never uploaded is answered with a 400 and is not completed"() {
        given:
        boolean completed = false
        Map service = [buildFromJSONFile: builds([fileWith("never.txt", null)]),
                       handleSubmission : { Map working -> completed = true; [] as HashSet }]

        when:
        Map answer = create(service)

        then:
        answer.status == 400
        answer.message == "never.txt: Not found or not exist or empty."
        !completed
    }

    void "a submission with an empty file is answered with a 400 and is not completed"() {
        given:
        boolean completed = false
        Map service = [buildFromJSONFile: builds([fileWith("empty.txt", "")]),
                       handleSubmission : { Map working -> completed = true; [] as HashSet }]

        when:
        Map answer = create(service)

        then:
        answer.status == 400
        answer.message == "empty.txt: Not found or not exist or empty."
        !completed
    }

    void "a submission with valid files goes on to be completed"() {
        given: "a service that stops the submission where it is completed, and a controller that copes with the failure"
        boolean completed = false
        Map service = [buildFromJSONFile: builds([fileWith("model.xml", "<sbml/>")]),
                       handleSubmission : { Map working -> completed = true; throw new IllegalStateException("stop here") },
                       cleanup          : { Map working -> }]
        controller.EXCH_DIR = dir.absolutePath
        controller.mailingService = [send: { Map mail -> }]
        controller.groovyPageRenderer = [render: { Map args -> "failed" }]

        when:
        Map answer = create(service)

        then:
        completed
        answer.status == "Failure"
    }

    // ------------------------------------------------------------------------ processUploadFiles

    /** What the upload step sends to processUploadFiles for a new model, with one model file. */
    private Map uploadModelFile(List<String> fileErrors, Closure inferModelFormat) {
        String uploads = '[{"id": "uploader1", "filename": "model-empty.xml", "description": "the model", ' +
            '"isModelFile": true, "originalFilesize": "0"}]'
        controller.EXCH_DIR = dir.absolutePath
        controller.fileSystemService = [retrieve: { def json -> new File(dir, json["filename"] as String) }]
        controller.submissionService = [validateFile  : { File f -> fileErrors },
                                        validateSyntax: { File f, String format, List errors -> true },
                                        detectModelInfo: { File f, String format -> [name: "A model"] }]
        controller.modelFileFormatService = [inferModelFormat: inferModelFormat,
                                             getPublicationAnnotations: { RTC revision -> [] }]
        params.submissionFolder = "a-submission-folder"
        params.uploadingFiles = uploads
        params.isUpdate = "false"
        controller.processUploadFiles()
        response.json as Map
    }

    void "a model file with errors is reported and nothing is detected in it"() {
        given: "the detection of the format fails on an empty xml file, as it did (SAXParseException)"
        boolean detected = false
        Closure inferModelFormat = { List files -> detected = true; throw new org.xml.sax.SAXParseException("Premature end of file.", null) }

        when:
        Map answer = uploadModelFile(["The file model-empty.xml is empty"], inferModelFormat)

        then:
        !detected
        Map file = answer.filesMap.first()
        file.validateFileErrors == ["The file model-empty.xml is empty"]
        file.validSyntax == false
        file.validateSyntaxErrors == []
        file.detectedModelFormat == [:]
        file.detectedModelInfo == [:]
    }

    void "a model file without errors has its format, syntax and information detected"() {
        given:
        boolean detected = false
        Closure inferModelFormat = { List files -> detected = true; new MFTC(id: 1, identifier: "UNKNOWN", name: "Unknown") }

        when:
        Map answer = uploadModelFile([], inferModelFormat)

        then:
        detected
        Map file = answer.filesMap.first()
        file.validateFileErrors == []
        file.detectedModelFormat.identifier == "UNKNOWN"
        file.validSyntax == true
        file.detectedModelInfo.name == "A model"
    }
}
