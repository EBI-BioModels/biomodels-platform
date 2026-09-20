package net.biomodels.jummp.webapp

import static org.junit.Assert.*
import grails.converters.JSON
import grails.util.GrailsWebUtil
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
}
