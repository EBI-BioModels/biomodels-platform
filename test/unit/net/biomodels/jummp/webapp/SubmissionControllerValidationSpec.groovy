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
 * SubmissionService refuses, in the API's create action and in the last validation of the submission wizard.
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
        given:
        List<String> messages = ["", "", ""]

        when:
        boolean valid = controller.doValidateUploadedFiles(
            [repository_files: [fileWith("a.xml", "<a/>"), fileWith("b.txt", "b")]], messages)

        then:
        valid
        messages[0] == ""
    }

    void "a missing file makes the files invalid and is named"() {
        given:
        List<String> messages = ["", "", ""]

        when:
        boolean valid = controller.doValidateUploadedFiles(
            [repository_files: [fileWith("a.xml", "<a/>"), fileWith("never.txt", null)]], messages)

        then:
        !valid
        messages[0] == "never.txt: Not found or not exist or empty.\n"
    }

    void "an empty file makes the files invalid and is named"() {
        given:
        List<String> messages = ["", "", ""]

        when:
        boolean valid = controller.doValidateUploadedFiles(
            [repository_files: [fileWith("empty.txt", ""), fileWith("a.xml", "<a/>")]], messages)

        then:
        !valid
        messages[0] == "empty.txt: Not found or not exist or empty.\n"
    }

    void "a directory makes the files invalid, even one with something in it, and is named"() {
        given: "a directory has a length, like a file"
        File folder = new File(dir, "a-folder")
        assert folder.mkdirs()
        new File(folder, "inside.txt").text = "not empty"
        List<String> messages = ["", "", ""]

        when:
        boolean valid = controller.doValidateUploadedFiles(
            [repository_files: [new RFTC(path: folder.path, mainFile: true), fileWith("a.xml", "<a/>")]], messages)

        then:
        folder.length() > 0
        !valid
        messages[0] == "a-folder: The model file cannot be a directory.\n"
    }

    void "the messages of a validation are those of that validation"() {
        given: "a model with a publication that is not valid, and one without a publication"
        MTC withPublication = new MTC(name: "A model", publication: new net.biomodels.jummp.core.model.PublicationTransportCommand())
        MTC withoutPublication = new MTC(name: "A model")
        Closure working = { MTC model, List<RFTC> files ->
            [repository_files: files, ModelTC: model, modelling_approach: "Other", submitterInfo: "[u, e]",
             submissionFolder: "a-folder", RevisionTC: new RTC(model: model, format: new MFTC(identifier: "UNKNOWN"))]
        }

        when:
        Map first = controller.doValidateSubmissionData(working(withPublication, [fileWith("a.xml", "<a/>")]))
        Map second = controller.doValidateSubmissionData(working(withoutPublication, [fileWith("b.xml", "<b/>")]))

        then:
        first.errMsg.trim() == "Publication record is invalid."
        !first.isPublicationValid
        second.errMsg == ""
        second.currentValidation
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

    void "a submission whose file is a directory is answered with a 400 and is not completed"() {
        given:
        File folder = new File(dir, "a-folder")
        assert folder.mkdirs()
        new File(folder, "inside.txt").text = "not empty"
        boolean completed = false
        Map service = [buildFromJSONFile: builds([new RFTC(path: folder.path, mainFile: true)]),
                       handleSubmission : { Map working -> completed = true; [] as HashSet }]

        when:
        Map answer = create(service)

        then:
        answer.status == 400
        answer.message == "a-folder: The model file cannot be a directory."
        !completed
    }

    void "a submission with valid files goes on to be completed, as it was built"() {
        given: "a service that stops the submission where it is completed, and a controller that copes with the failure"
        Map completed = null
        Map service = [buildFromJSONFile: builds([fileWith("model.xml", "<sbml/>")]),
                       handleSubmission : { Map working -> completed = working; throw new IllegalStateException("stop here") },
                       cleanup          : { Map working -> }]
        controller.EXCH_DIR = dir.absolutePath
        controller.mailingService = [send: { Map mail -> }]
        controller.groovyPageRenderer = [render: { Map args -> "failed" }]

        when:
        Map answer = create(service)

        then:
        completed != null
        completed.containsKey("RevisionTC")
        completed.repository_files*.path == [new File(dir, "model.xml").path]
        answer.status == "Failure"
    }

    // ------------------------------------------------------------------------ the submission wizard, JBM-802

    /** Puts a file in the submission folder of the exchange directory, like the upload step. */
    private void putInFolder(String folder, String name, String text) {
        File where = new File(dir, folder)
        where.mkdirs()
        new File(where, name).text = text
    }

    /** What the wizard sends to doLastValidateSubmissionData and to completeSubmission for a new model. */
    private void wizardSends(String folder, String filename, String modelName = "A model") {
        params.clear()
        params.putAll([
            submitterInfo: "[testuser, test@test.com]", submissionFolder: folder,
            modelFile: '{"submissionFolder": "' + folder + '", "filename": "' + filename + '", "description": "the model"}',
            additionalFiles: "[]", isUpdate: "false", isAmend: "false", isMinorRevision: "false",
            isMetadataSubmission: "false", modelInfo: '{"detectedName": "' + modelName + '"}',
            publication: '""', revisionComments: "", latestContributorRole: "Modeller"])
    }

    /** A controller whose service completes nothing: it keeps the data that it is given and stops there. */
    private void wizardController(List<Map> completed) {
        controller.EXCH_DIR = dir.absolutePath
        controller.modelFileFormatService = [inferModelFormat: { List files -> new MFTC(identifier: "UNKNOWN", name: "Unknown") }]
        controller.submissionService = [handleSubmission: { Map working -> completed << working; throw new IllegalStateException("stop here") },
                                        cleanup         : { Map working -> }]
        controller.mailingService = [send: { Map mail -> }]
        controller.groovyPageRenderer = [render: { Map args -> "failed" }]
    }

    void "the completion completes the data that it was sent, not the data that the last validation kept"() {
        given: "two submissions, and the last validation is the one of the second"
        List<Map> completed = []
        wizardController(completed)
        putInFolder("first-folder", "first.txt", "the first file")
        putInFolder("second-folder", "second.txt", "the second file")
        wizardSends("first-folder", "first.txt", "The first model")
        controller.doLastValidateSubmissionData()
        response.reset()
        wizardSends("second-folder", "second.txt", "The second model")
        controller.doLastValidateSubmissionData()
        response.reset()

        when: "the first one is completed"
        wizardSends("first-folder", "first.txt", "The first model")
        controller.completeSubmission()

        then:
        completed.size() == 1
        completed[0].repository_files*.path == [new File(new File(dir, "first-folder"), "first.txt").canonicalPath]
        completed[0].submissionFolder == "first-folder"
        completed[0].ModelTC.name == "The first model"
    }

    void "the completion completes what nothing validated before"() {
        given:
        List<Map> completed = []
        wizardController(completed)
        putInFolder("a-folder", "model.txt", "the model")
        wizardSends("a-folder", "model.txt")

        when:
        controller.completeSubmission()

        then:
        completed.size() == 1
        completed[0].repository_files*.path == [new File(new File(dir, "a-folder"), "model.txt").canonicalPath]
    }

    void "the completion does not complete a submission with an empty file, whatever was validated before"() {
        given: "a valid submission that the last validation kept, and an empty file in the submission to be completed"
        List<Map> completed = []
        wizardController(completed)
        putInFolder("valid-folder", "valid.txt", "the file")
        putInFolder("empty-folder", "empty.txt", "")
        wizardSends("valid-folder", "valid.txt")
        controller.doLastValidateSubmissionData()
        response.reset()

        when:
        wizardSends("empty-folder", "empty.txt")
        controller.completeSubmission()

        then:
        completed.isEmpty()
        response.json.status == "Failure"
        response.json.message == "<div style='color: red'>empty.txt: Not found or not exist or empty.</div>"
    }

    void "the completion does not complete a submission whose file is missing"() {
        given:
        List<Map> completed = []
        wizardController(completed)
        new File(dir, "a-folder").mkdirs()
        wizardSends("a-folder", "never.txt")

        when:
        controller.completeSubmission()

        then:
        completed.isEmpty()
        response.json.status == "Failure"
        response.json.message.contains("never.txt: Not found or not exist or empty.")
    }

    void "the message of a refused completion is html, with the names of the files escaped"() {
        given:
        List<Map> completed = []
        wizardController(completed)
        putInFolder("a-folder", "a<b>.txt", "")
        wizardSends("a-folder", "a<b>.txt")

        when:
        controller.completeSubmission()

        then:
        completed.isEmpty()
        response.json.message == "<div style='color: red'>a&lt;b&gt;.txt: Not found or not exist or empty.</div>"
    }

    // ------------------------------------------------------------------------ processUploadFiles

    /**
     * What the upload step sends to processUploadFiles for a new model. The errors that validateFile finds are the same
     * for every file, or the result of a closure that gets the file.
     */
    private Map uploadFiles(List<Map> uploads, def fileErrors, Closure inferModelFormat) {
        controller.EXCH_DIR = dir.absolutePath
        controller.fileSystemService = [retrieve: { def json -> new File(dir, json["filename"] as String) }]
        controller.submissionService = [validateFile  : { File f -> fileErrors instanceof Closure ? fileErrors(f) : fileErrors },
                                        validateSyntax: { File f, String format, List errors -> true },
                                        detectModelInfo: { File f, String format -> [name: "A model"] }]
        controller.modelFileFormatService = [inferModelFormat: inferModelFormat,
                                             getPublicationAnnotations: { RTC revision -> [] }]
        params.submissionFolder = "a-submission-folder"
        params.uploadingFiles = new groovy.json.JsonBuilder(uploads).toString()
        params.isUpdate = "false"
        controller.processUploadFiles()
        response.json as Map
    }

    private static Map upload(String filename, boolean isModelFile, String description = "the file") {
        [id: "uploader-$filename".toString(), filename: filename, description: description, isModelFile: isModelFile,
         originalFilesize: "0"]
    }

    /** A model file, model-empty.xml, as the upload step sends it. */
    private Map uploadModelFile(List<String> fileErrors, Closure inferModelFormat) {
        uploadFiles([upload("model-empty.xml", true, "the model")], fileErrors, inferModelFormat)
    }

    void "a model file with errors is reported and nothing is detected in it"() {
        given: "the detection of the format fails on an empty xml file, as it did (SAXParseException)"
        boolean detected = false
        Closure inferModelFormat = { List files -> detected = true; throw new org.xml.sax.SAXParseException("Premature end of file.", null) }

        when:
        Map answer = uploadModelFile(["The file is empty"], inferModelFormat)

        then:
        !detected
        Map file = answer.filesMap.first()
        file.validateFileErrors == ["The file is empty"]
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

    private static final Closure UNKNOWN_FORMAT = { List files -> new MFTC(id: 1, identifier: "UNKNOWN", name: "Unknown") }

    void "each file without a description says so, and one with a description does not"() {
        when:
        Map answer = uploadFiles([upload("model.txt", true, "the model"), upload("a.txt", false, ""),
                                  upload("b.txt", false, "   "), upload("c.txt", false, "notes")], [], UNKNOWN_FORMAT)

        then:
        answer.filesMap*.validateFileDescription == [[], ["The file needs a description"],
                                                     ["The file needs a description"], []]
    }

    void "a model file without a description is still detected"() {
        given:
        boolean detected = false
        Closure inferModelFormat = { List files -> detected = true; new MFTC(id: 1, identifier: "UNKNOWN", name: "Unknown") }

        when:
        Map answer = uploadFiles([upload("model.txt", true, "")], [], inferModelFormat)

        then:
        detected
        answer.filesMap.first().validateFileDescription == ["The file needs a description"]
        answer.filesMap.first().detectedModelFormat.identifier == "UNKNOWN"
    }

    void "a file with an invalid name says so, and one with a valid name does not"() {
        when:
        Map answer = uploadFiles([upload("model.txt", true), upload("data (1).txt", false), upload("no-extension", false),
                                  upload("my data-1+2_3.txt", false)], [], UNKNOWN_FORMAT)

        then:
        answer.filesMap*.validateFileName == [null, [SubmissionController.FILE_NAME_INVALID],
                                              [SubmissionController.FILE_NAME_INVALID], null]
        SubmissionController.FILE_NAME_INVALID.startsWith("The file name is invalid")
    }

    void "a file that is empty, without a description and with an invalid name has each problem said on its own"() {
        when:
        Map answer = uploadFiles([upload("data (1).txt", false, "")], ["The file is empty"], UNKNOWN_FORMAT)

        then:
        Map file = answer.filesMap.first()
        file.validateFileErrors == ["The file is empty"]
        file.validateFileDescription == ["The file needs a description"]
        file.validateFileName == [SubmissionController.FILE_NAME_INVALID]
        file.validateFileSummary == "The file is empty. The file needs a description. " +
            SubmissionController.FILE_NAME_INVALID + "."
    }

    void "the problems of a file are said in one line, and a file without problems has none"() {
        given: "the file that is empty is model-empty.xml"
        Closure validateFile = { File f -> f.name == "model-empty.xml" ? ["The file is empty"] : [] }

        when:
        Map answer = uploadFiles([upload("Zhou2024_Updated-model.m", true, ""), upload("model-empty.xml", false, ""),
                                  upload("fine.txt", false, "notes")], validateFile, UNKNOWN_FORMAT)

        then:
        answer.filesMap*.validateFileSummary == ["The file needs a description.",
                                                 "The file is empty. The file needs a description.", ""]
    }

    void "messages become sentences, once"() {
        expect:
        SubmissionController.sentences(messages) == sentence

        where:
        messages                                  | sentence
        []                                        | ""
        ["The file is empty"]                     | "The file is empty."
        ["The file is empty."]                    | "The file is empty."
        [" The file is empty ", "It has no name"] | "The file is empty. It has no name."
        ["Is it empty?", "Too big!"]              | "Is it empty? Too big!"
    }
}
