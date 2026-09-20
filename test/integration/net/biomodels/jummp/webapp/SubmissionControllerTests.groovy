package net.biomodels.jummp.webapp

import static org.junit.Assert.*
import grails.converters.JSON
import grails.util.GrailsWebUtil
import groovy.json.JsonBuilder
import net.biomodels.jummp.core.SubmissionRouteTestBase
import net.biomodels.jummp.model.Model
import org.junit.*

/**
 * Covers JBM-793 and JBM-796: the create and update actions of SubmissionController, which the API calls and which
 * hand a submission to SubmissionService (SubmissionApiRouteTests tests the service without the controller).
 *
 * The actions answer with a JSON body that holds a status of its own, "Success", "Failure" or the number of the HTTP
 * status that they mean, and not the status of the response.
 */
class SubmissionControllerTests extends SubmissionRouteTestBase {
    /**
     * Calls an action like the API does: the metadata of the submission as the body of the request, the folder the
     * files were uploaded to as a header. Every call has a request, a response and a controller of its own, as it has
     * in production. Returns what the action rendered.
     */
    private Map call(String action, String metadata, String folder = null) {
        GrailsWebUtil.bindMockWebRequest(grailsApplication.mainContext)
        SubmissionController controller = grailsApplication.mainContext.getBean(SubmissionController.name)
        // the controller reads the exchange directory once, when it is created, and it is the developer's real one in a
        // test. It writes there, to buggy/<folder>, when a submission fails.
        controller.EXCH_DIR = exchange.absolutePath
        controller.request.method = "POST"
        controller.request.content = metadata.getBytes("UTF-8")
        if (folder) {
            controller.request.addHeader("SubmissionFolder", folder)
        }
        controller.params.format = "json"
        controller."$action"()
        assertEquals(200, controller.response.status)
        JSON.parse(controller.response.contentAsString) as Map
    }

    private String createModel() {
        String folder = stage(["mainFile.txt": "the first main file", "addFile.txt": "the first additional file"])
        Map result = call("create", metadata([
            name: "A model to update", description: "It has two files", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]],
                    additional: [[name: "addFile.txt", description: "the additional file"]]]]), folder)
        assertEquals("Success", result.status)
        result.modelIdentifier
    }

    // ------------------------------------------------------------------------------------------------ create

    @Test
    void testCreateAModel() {
        String folder = stage(["mainFile.txt": "this is a main file"])

        Map result = call("create", metadata([
            name: "A model of nothing", description: "It has no format", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]], additional: []]]), folder)

        assertEquals("Success", result.status)
        String modelId = result.modelIdentifier
        assertNotNull(modelId)
        assertTrue(result.modelURL.toString().endsWith("/$modelId".toString()))
        assertEquals("A model of nothing", modelService.getLatestRevision(modelService.getModel(modelId)).name)
        assertEquals("this is a main file", modelService.retrieveFiles(
            modelService.getLatestRevision(modelService.getModel(modelId))).find { it.name == "mainFile.txt" }.text)
        // the files are moved out of the exchange directory into the repository
        assertFalse(new File(exchange, folder).exists())
    }

    @Test
    void testCreateWithoutAnyInputIsRefused() {
        Map result = call("create", "")

        assertEquals(400, result.status)
        assertEquals("Cannot create the model as requested because of the empty input.", result.message)
        assertEquals(0, Model.count())
    }

    // ------------------------------------------------------------------------------------------------ update

    @Test
    void testUpdateAModel() {
        String modelId = createModel()
        String folder = stage(["mainFile.txt": "the second main file", "addFile.txt": "the second additional file"])

        Map result = call("update", metadata([
            submissionId: modelId, name: "A model to update", description: "It has two files", format: format("UNKNOWN"),
            comment: "An update",
            files: [main: [[name: "mainFile.txt", description: "the main file"]],
                    additional: [[name: "addFile.txt", description: "the additional file"]]]]), folder)

        assertEquals("Success", result.status)
        assertEquals(modelId, result.modelIdentifier)
        def revision = modelService.getLatestRevision(modelService.getModel(modelId))
        assertEquals(2, revision.revisionNumber)
        assertEquals("the second main file",
            modelService.retrieveFiles(revision).find { it.name == "mainFile.txt" }.text)
    }

    @Test
    void testUpdateWithoutAnyInputIsRefused() {
        Map result = call("update", "")

        assertEquals(400, result.status)
        assertEquals("Cannot update the model as requested because of the empty input.", result.message)
    }

    // ---------------------------------------------------------------------------------- what is rejected

    // JBM-796: makeSubmission used to read the repository files of the working memory in a finally block, and
    // SubmissionService.buildFromJSONFile has not put them there when it rejects a submission, so the action threw a
    // NullPointerException, which the API answers with a 500, instead of rendering a 400 with the reason.

    @Test
    void testCreateWithoutFilesIsRefused() {
        Map result = call("create", metadata([
            name: "No files", format: format("UNKNOWN"), files: [main: [], additional: []]]), stage([:]))

        assertEquals(400, result.status)
        assertEquals("Cannot find the model files. The submission process has to be terminated!", result.message)
        assertEquals(0, Model.count())
    }

    @Test
    void testCreateWithoutAModelNameIsRefused() {
        String folder = stage(["mainFile.txt": "a main file"])

        Map result = call("create", metadata([
            name: "", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]], additional: []]]), folder)

        assertEquals(400, result.status)
        assertEquals("Cannot leave the model name and format properties empty!", result.message)
        assertEquals(0, Model.count())
    }

    @Test
    void testUpdateWithoutTheModelIdentifierIsRefused() {
        createModel()
        String folder = stage(["mainFile.txt": "a main file"])

        Map result = call("update", metadata([
            name: "Whatever", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]], additional: []]]), folder)

        assertEquals(400, result.status)
        assertEquals("The model identifier is missing.", result.message)
        assertEquals(1, Model.count())
    }

    @Test
    void testCreateFromMetadataThatIsNotJsonIsRefused() {
        Map result = call("create", "this is not json", stage([:]))

        assertEquals(400, result.status)
        assertFalse(result.message.toString().isEmpty())
        assertEquals(0, Model.count())
    }

    // ------------------------------------------------------------------ what the validation of the files refuses
    // JBM-798: the controller validates the files before it completes a submission, as the web wizard does. A file that
    // is missing and a file that is empty both make the files invalid.

    @Test
    void testCreateWithAFileThatWasNeverUploadedIsRefused() {
        String folder = stage(["mainFile.txt": "the main file"])

        Map result = call("create", metadata([
            name: "A file is missing", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]],
                    additional: [[name: "addFile.txt", description: "never uploaded"]]]]), folder)

        assertEquals(400, result.status)
        assertEquals("addFile.txt: Not found or not exist or empty.", result.message)
        assertEquals(0, Model.count())
        // it is the submitter's mistake and not a bug: no ticket, no copy of the folder for the developers
        assertFalse(result.containsKey("ticketID"))
        assertFalse(new File(exchange, "buggy/$folder").exists())
    }

    @Test
    void testCreateWithAnEmptyFileIsRefused() {
        String folder = stage(["mainFile.txt": ""])

        Map result = call("create", metadata([
            name: "An empty file", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]], additional: []]]), folder)

        assertEquals(400, result.status)
        assertEquals("mainFile.txt: Not found or not exist or empty.", result.message)
        assertEquals(0, Model.count())
    }

    @Test
    void testUpdateWithAnEmptyAdditionalFileIsRefused() {
        String modelId = createModel()
        String folder = stage(["mainFile.txt": "the second main file", "addFile.txt": ""])

        Map result = call("update", metadata([
            submissionId: modelId, name: "A model to update", description: "It has two files", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]],
                    additional: [[name: "addFile.txt", description: "the additional file"]]]]), folder)

        assertEquals(400, result.status)
        assertEquals("addFile.txt: Not found or not exist or empty.", result.message)
        assertEquals(1, modelService.getLatestRevision(modelService.getModel(modelId)).revisionNumber)
    }

    @Test
    void testCreateWithADirectoryAsAFileIsRefused() {
        // a directory has a length, so it passed the check of the files
        String folder = stage(["mainFile.txt": "the main file"])
        File directory = new File(new File(exchange, folder), "a-directory")
        assertTrue directory.mkdirs()
        new File(directory, "inside.txt").text = "not empty"

        Map result = call("create", metadata([
            name: "A directory", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]],
                    additional: [[name: "a-directory", description: "a directory"]]]]), folder)

        assertEquals(400, result.status)
        assertEquals("a-directory: The model file cannot be a directory.", result.message)
        assertEquals(0, Model.count())
        assertFalse(result.containsKey("ticketID"))
    }

    @Test
    void testUpdateWithAFileThatWasNeverUploadedIsRefused() {
        String modelId = createModel()
        String folder = stage(["addFile.txt": "the second additional file"])

        Map result = call("update", metadata([
            submissionId: modelId, name: "A model to update", description: "It has two files", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "never uploaded"]],
                    additional: [[name: "addFile.txt", description: "the additional file"]]]]), folder)

        assertEquals(400, result.status)
        assertEquals("mainFile.txt: Not found or not exist or empty.", result.message)
        // the model is as it was
        assertEquals(1, modelService.getLatestRevision(modelService.getModel(modelId)).revisionNumber)
    }

    // ---------------------------------------- the last validation and the completion of the submission wizard (JBM-802)

    /**
     * What the wizard's Submit button sends to doLastValidateSubmissionData and what its last step sends to
     * completeSubmission: the same data, for a new model, or for the update of a model. Returns what the action renders.
     */
    private Map wizard(String action, String folder, String filename, String name = "A model",
                       Map extra = [:], String additional = "[]") {
        GrailsWebUtil.bindMockWebRequest(grailsApplication.mainContext)
        SubmissionController controller = grailsApplication.mainContext.getBean(SubmissionController.name)
        controller.EXCH_DIR = exchange.absolutePath
        controller.request.method = "POST"
        controller.params.putAll([
            submitterInfo: "[testuser, test@test.com]", submissionFolder: folder,
            modelFile: '{"submissionFolder": "' + folder + '", "filename": "' + filename + '", "description": "the model"}',
            additionalFiles: additional, isUpdate: "false", isAmend: "false", isMinorRevision: "false",
            isMetadataSubmission: "false", modelInfo: '{"detectedName": "' + name + '", "detectedDescription": "It has a file"}',
            publication: '""', revisionComments: "", latestContributorRole: "Modeller"])
        controller.params.putAll(extra)
        controller."$action"()
        JSON.parse(controller.response.contentAsString) as Map
    }

    private Map validateLastStepOfWizard(String folder, String filename, String name = "A model") {
        wizard("doLastValidateSubmissionData", folder, filename, name)
    }

    private Map completeInWizard(String folder, String filename, String name = "A model") {
        wizard("completeSubmission", folder, filename, name)
    }

    private String mainFileOf(String modelId) {
        modelService.retrieveFiles(modelService.getLatestRevision(modelService.getModel(modelId)))
            .find { it.name == "mainFile.txt" }.text
    }

    @Test
    void testTheWizardAcceptsAModelFile() {
        String folder = stage(["mainFile.txt": "the main file"])

        Map result = validateLastStepOfWizard(folder, "mainFile.txt")

        assertTrue(result.areModelFilesValid)
        assertTrue(result.currentValidation)
        // it only validates: the model is created by the request that completes the submission
        assertEquals(0, Model.count())
    }

    @Test
    void testTheWizardCompletesTheSubmissionThatItWasSent() {
        String first = stage(["mainFile.txt": "the first main file"])
        String second = stage(["mainFile.txt": "the second main file"])
        // two users are on the last step. The second validates after the first, and the first completes after that:
        // a controller that kept the data of the last validation would complete the submission of the second user
        assertTrue(validateLastStepOfWizard(first, "mainFile.txt", "The first model").currentValidation)
        assertTrue(validateLastStepOfWizard(second, "mainFile.txt", "The second model").currentValidation)

        Map result = completeInWizard(first, "mainFile.txt", "The first model")

        assertEquals("Success", result.status)
        assertEquals(1, Model.count())
        String modelId = result.modelIdentifier
        assertEquals("The first model", modelService.getLatestRevision(modelService.getModel(modelId)).name)
        assertEquals("the first main file", mainFileOf(modelId))
        // the other submission is where its submitter left it
        assertTrue(new File(new File(exchange, second), "mainFile.txt").exists())
    }

    @Test
    void testTheWizardCompletesASubmissionThatNoRequestValidatedBefore() {
        String folder = stage(["mainFile.txt": "the main file"])

        Map result = completeInWizard(folder, "mainFile.txt", "A model nobody validated")

        assertEquals("Success", result.status)
        assertEquals("the main file", mainFileOf(result.modelIdentifier))
    }

    @Test
    void testTheWizardDoesNotCompleteASubmissionThatIsNotValid() {
        String valid = stage(["mainFile.txt": "the main file"])
        String empty = stage(["mainFile.txt": ""])
        // the last validation of somebody else, which a completion that reads it from the controller would complete
        assertTrue(validateLastStepOfWizard(valid, "mainFile.txt").currentValidation)

        Map result = completeInWizard(empty, "mainFile.txt")

        assertEquals("Failure", result.status)
        assertTrue(result.message.toString().contains("mainFile.txt: Not found or not exist or empty."))
        assertEquals(0, Model.count())
        assertFalse(result.containsKey("ticketID"))
    }

    @Test
    void testTheWizardDoesNotCompleteASubmissionWhoseFileIsMissing() {
        String folder = stage([:])

        Map result = completeInWizard(folder, "mainFile.txt")

        assertEquals("Failure", result.status)
        assertEquals(0, Model.count())
    }

    @Test
    void testTheRefusalOfTheWizardIsSafeToShowAsHtml() {
        // the page puts the message in the document as it is
        String folder = stage(["a<b>.txt": ""])

        Map result = completeInWizard(folder, "a<b>.txt")

        assertEquals("Failure", result.status)
        assertFalse(result.message.toString().contains("<b>"))
        assertTrue(result.message.toString().contains("a&lt;b&gt;.txt"))
    }

    @Test
    void testTheWizardCompletesTheUpdateThatItWasSent() {
        String modelId = createModel()
        String update = stage(["mainFile.txt": "the updated main file", "addFile.txt": "the first additional file"])
        // the data of an update that the wizard sends: the model, what it was and what the submitter changed
        Map extra = [isUpdate: "true", modelId: modelId, latestModelName: "A model to update",
                     "changesMade[]": ["The main file was changed"]]
        String additional = '[{"submissionFolder": "' + update + '", "filename": "addFile.txt", "description": "more"}]'
        Map validation = wizard("doLastValidateSubmissionData", update, "mainFile.txt", "A model to update", extra, additional)
        assertTrue(validation.currentValidation)

        Map result = wizard("completeSubmission", update, "mainFile.txt", "A model to update", extra, additional)

        assertEquals("Success", result.status)
        assertEquals(modelId, result.modelIdentifier)
        assertEquals(2, modelService.getLatestRevision(modelService.getModel(modelId)).revisionNumber)
        assertEquals("the updated main file", mainFileOf(modelId))
    }

    @Test
    void testTheWizardDoesNotAcceptAnEmptyModelFile() {
        String folder = stage(["mainFile.txt": ""])

        Map result = validateLastStepOfWizard(folder, "mainFile.txt")

        assertFalse(result.areModelFilesValid)
        assertFalse(result.currentValidation)
        assertEquals("mainFile.txt: Not found or not exist or empty.", result.errMsg.toString().trim())
    }

    @Test
    void testTheWizardDoesNotAcceptAMissingModelFile() {
        String folder = stage([:])

        Map result = validateLastStepOfWizard(folder, "mainFile.txt")

        assertFalse(result.areModelFilesValid)
        assertFalse(result.currentValidation)
    }

    // ------------------------------------------------------------------- the upload step of the web wizard

    /** What the upload step sends to processUploadFiles for a new model, with the files. Returns what it renders. */
    private Map uploadFiles(String folder, List<Map> uploads) {
        GrailsWebUtil.bindMockWebRequest(grailsApplication.mainContext)
        SubmissionController controller = grailsApplication.mainContext.getBean(SubmissionController.name)
        controller.EXCH_DIR = exchange.absolutePath
        controller.request.method = "POST"
        controller.params.putAll([submissionFolder: folder, isUpdate: "false",
                                  uploadingFiles  : new JsonBuilder(uploads).toString()])
        // FileSystemService.retrieve looks in the exchange directory of the properties file, the developer's own
        withExchangeDirectoryOfTheFileSystemService { controller.processUploadFiles() }
        JSON.parse(controller.response.contentAsString) as Map
    }

    private static Map upload(String filename, boolean isModelFile, String description = "the file") {
        [id: "uploader-$filename".toString(), filename: filename, description: description, isModelFile: isModelFile,
         originalFilesize: "0"]
    }

    /** What the upload step sends for one model file. Returns what processUploadFiles renders. */
    private Map uploadModelFile(String folder, String filename) {
        uploadFiles(folder, [upload(filename, true, "the model file")])
    }

    @Test
    void testUploadingAnEmptyXmlFileReportsItAndDetectsNothing() {
        // the detection of the format cannot read an empty xml file: it threw a SAXParseException, and the wizard
        // got a 500 and no message
        String folder = stage(["model-empty.xml": ""])

        Map result = uploadModelFile(folder, "model-empty.xml")

        Map file = result.filesMap.first()
        assertEquals(["The file is empty"], file.validateFileErrors)
        assertFalse(file.validSyntax)
        assertEquals([], file.validateSyntaxErrors)
        // what the upload step reads is there, and empty
        assertEquals([:], file.detectedModelFormat)
        assertEquals([:], file.detectedModelInfo)
    }

    @Test
    void testUploadingAFileThatIsNotThereReportsIt() {
        String folder = stage([:])

        Map result = uploadModelFile(folder, "never.xml")

        assertEquals(["File does not exist"], result.filesMap.first().validateFileErrors)
    }

    @Test
    void testUploadingAModelFileDetectsItsFormat() {
        String folder = stage(["model.txt": "what is this?"])

        Map result = uploadModelFile(folder, "model.txt")

        Map file = result.filesMap.first()
        assertEquals([], file.validateFileErrors)
        assertEquals("UNKNOWN", file.detectedModelFormat.identifier)
        assertNotNull(file.detectedModelInfo)
    }

    @Test
    void testEveryFileSaysWhatIsWrongWithIt() {
        // an empty model file and an empty additional file, each without a description and with an invalid name, and
        // a model file that is fine: each problem is said on its own, for the file that has it
        String folder = stage(["model-empty.xml": "", "data (1).txt": "", "fine.txt": "some content"])

        Map result = uploadFiles(folder, [upload("model-empty.xml", true, ""), upload("data (1).txt", false, ""),
                                          upload("fine.txt", false, "notes")])

        Map empty = result.filesMap.find { it.filename == "model-empty.xml" }
        assertEquals(["The file is empty"], empty.validateFileErrors)
        assertEquals(["The file needs a description"], empty.validateFileDescription)
        assertNull(empty.validateFileName)
        Map data = result.filesMap.find { it.filename == "data (1).txt" }
        assertEquals(["The file is empty"], data.validateFileErrors)
        assertEquals(["The file needs a description"], data.validateFileDescription)
        assertEquals([SubmissionController.FILE_NAME_INVALID], data.validateFileName)
        Map fine = result.filesMap.find { it.filename == "fine.txt" }
        assertEquals([], fine.validateFileErrors)
        assertEquals([], fine.validateFileDescription)
        assertNull(fine.validateFileName)
        // and the upload step shows one line for each file, with the name of the file in front of it
        assertEquals("The file is empty. The file needs a description.", empty.validateFileSummary)
        assertEquals("The file is empty. The file needs a description. " +
            SubmissionController.FILE_NAME_INVALID + ".", data.validateFileSummary)
        assertEquals("", fine.validateFileSummary)
    }

    @Test
    void testTheProblemsOfAFileAreSaidInOneLine() {
        // a model file that has no description, and an empty additional file that has none either
        String folder = stage(["Zhou2024_Updated-model.m": "a model", "model-empty.xml": ""])

        Map result = uploadFiles(folder, [upload("Zhou2024_Updated-model.m", true, ""),
                                          upload("model-empty.xml", false, "")])

        assertEquals(["The file needs a description.",
                      "The file is empty. The file needs a description."],
            result.filesMap*.validateFileSummary)
    }

    @Test
    void testAModelFileWithoutADescriptionIsStillDetected() {
        String folder = stage(["model.txt": "what is this?"])

        Map result = uploadFiles(folder, [upload("model.txt", true, "")])

        Map file = result.filesMap.first()
        assertEquals(["The file needs a description"], file.validateFileDescription)
        assertEquals("UNKNOWN", file.detectedModelFormat.identifier)
    }
}
