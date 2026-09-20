package net.biomodels.jummp.core

import static org.junit.Assert.*
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.model.ModelAudit
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.PublicationLinkProvider
import net.biomodels.jummp.plugins.configuration.VcsCommand
import org.junit.*

/**
 * Covers JBM-793: the route a submission takes through SubmissionService when it comes in through
 * SubmissionController (the API's create and update actions, and the web wizard's validation), i.e.
 * buildFromJSONFile, validateFile, validateSyntax, detectModelInfo, handleSubmission, processPostSubmission and cleanup,
 * and the initialise with which ModelController starts a submission in the web wizard. The old SubmissionServiceTests
 * drove another set of methods, the steps of the Webflow that JBM-527 deleted.
 *
 * SubmissionControllerTests does the same through the controller itself. See SubmissionRouteTestBase for how the files
 * of a test are staged.
 */
class SubmissionApiRouteTests extends SubmissionRouteTestBase {

    /** The working memory SubmissionController starts a create or an update with. */
    private Map newWorking(String submissionFolder, boolean isUpdate = false) {
        def user = springSecurityService.currentUser
        [isUpdate: isUpdate, isUpdateOnExistingModel: isUpdate, isAmend: false, isMetadataSubmission: false,
         accessType: isUpdate ? "update" : "create", accessFormat: "json", submissionFolder: submissionFolder,
         submitterInfo: [userRealName: user.person?.userRealName, username: user.username, email: user.email]
        ] as HashMap<String, Object>
    }

    /** What SubmissionController.makeSubmission does with the service for a create. Returns the model's identifier. */
    private String create(Map working, String json) {
        submissionService.buildFromJSONFile(json, working)
        Set<String> result = submissionService.handleSubmission(working)
        String modelId = result.first()
        working.putAll([modelId: modelId, accessType: "create", changesMade: []])
        submissionService.processPostSubmission(working)
        modelId
    }

    private List<File> filesOf(String modelId) {
        modelService.retrieveFiles(modelService.getLatestRevision(modelService.getModel(modelId)))
    }

    // ---------------------------------------------------------------------------------------------- new models

    @Test
    void testCreateModel() {
        String folder = stage(["mainFile.txt": "this is a main file", "addFile.txt": "this is an additional file"])
        Map working = newWorking(folder)
        String json = metadata([
            name: "A model of nothing", description: "It has no format",
            format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]],
                    additional: [[name: "addFile.txt", description: "the additional file"]]]])

        String modelId = create(working, json)

        assertNotNull(modelId)
        List<File> files = filesOf(modelId)
        assertEquals("this is a main file", files.find { it.name == "mainFile.txt" }.text)
        assertEquals("this is an additional file", files.find { it.name == "addFile.txt" }.text)
        assertEquals("A model of nothing", modelService.getLatestRevision(modelService.getModel(modelId)).name)
        // handleSubmission removes the staged files and their folder
        assertFalse(new File(exchange, folder).exists())
    }

    // JBM-795: processPostSubmission cannot find the new model, because the command object in the working memory has no
    // id yet, and returns -1 instead of the identifier of the audit item
    @Ignore("JBM-795: creating a model records no audit item")
    @Test
    void testCreateModelIsRecorded() {
        String folder = stage(["mainFile.txt": "this is a main file"])
        String modelId = create(newWorking(folder), metadata([
            name: "A model to audit", description: "It has one file", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]], additional: []]]))

        assertEquals(1, ModelAudit.countByModel(Model.findBySubmissionId(modelId)))
    }

    /** Submits a model that is a single file, as the API's create action does. Returns the model's identifier. */
    private String createFrom(File fixture, String identifier, String version, String name) {
        String folder = stageFile(fixture)
        String json = metadata([
            name: name, description: "A test of $identifier",
            format: format(identifier, version),
            files: [main: [[name: fixture.name, description: "the model"]], additional: []]])
        create(newWorking(folder), json)
    }

    private Revision latestRevisionOf(String modelId) {
        modelService.getLatestRevision(modelService.getModel(modelId))
    }

    // ------------------------------------------------------------------------------------ one of each format

    @Test
    void testCreateSbmlModel() {
        File sbml = new File("jummp-plugins/jummp-plugin-sbml/test/files/Koch2017.xml")
        String modelId = createFrom(sbml, "SBML", "L3V1", "Koch 2017")

        Revision revision = latestRevisionOf(modelId)
        assertEquals("SBML", revision.format.identifier)
        assertEquals("L3V1", revision.format.formatVersion)
        assertEquals(["Koch2017.xml"], filesOf(modelId)*.name)
    }

    @Test
    void testCreatePharmMlModel() {
        File pharmml = new File("jummp-plugins/jummp-plugin-pharmml/test/files/0.3.1/example1.xml")
        String modelId = createFrom(pharmml, "PharmML", "0.3.1", "Example 1")

        Revision revision = latestRevisionOf(modelId)
        assertEquals("PharmML", revision.format.identifier)
        assertEquals("0.3.1", revision.format.formatVersion)
        assertEquals(["example1.xml"], filesOf(modelId)*.name)
    }

    @Test
    void testCreateMdlModel() {
        File mdl = new File("jummp-plugins/jummp-plugin-mdl/test/files/alzheimer/Alzheimer.mdl")
        String modelId = createFrom(mdl, "MDL", "*", "Alzheimer")

        assertEquals("MDL", latestRevisionOf(modelId).format.identifier)
        assertEquals(["Alzheimer.mdl"], filesOf(modelId)*.name)
    }

    @Test
    void testCreateCombineArchive() {
        // built here: the sample archive of the COMBINE Archive plugin is one of the fixtures other tests move
        File archive = new File(exchange, "sample.omex")
        File model = new File("jummp-plugins/jummp-plugin-sbml/test/files/Koch2017.xml")
        archive.withOutputStream { out ->
            new java.util.zip.ZipOutputStream(out).withCloseable { zip ->
                zip.putNextEntry(new java.util.zip.ZipEntry("manifest.xml"))
                zip << '''<?xml version="1.0" encoding="UTF-8"?>
<omexManifest xmlns="http://identifiers.org/combine.specifications/omex-manifest">
  <content location="." format="http://identifiers.org/combine.specifications/omex"/>
  <content location="./model.xml" format="http://identifiers.org/combine.specifications/sbml"/>
</omexManifest>'''
                zip.closeEntry()
                zip.putNextEntry(new java.util.zip.ZipEntry("model.xml"))
                zip << model.bytes
                zip.closeEntry()
            }
        }
        String modelId = createFrom(archive, "OMEX", "*", "Koch 2017 archive")

        assertEquals("OMEX", latestRevisionOf(modelId).format.identifier)
        assertEquals(["sample.omex"], filesOf(modelId)*.name)
    }

    @Test
    void testAFormatThatIsNotRegisteredIsSubmittedAsOther() {
        String folder = stage(["model.txt": "what is this?"])
        Map working = newWorking(folder)
        String json = metadata([
            name: "Unheard of", description: "", format: [name: "Unheard of", version: "1"],
            files: [main: [[name: "model.txt", description: "the model"]], additional: []]])

        submissionService.buildFromJSONFile(json, working)

        assertEquals("UNKNOWN", working["format"].identifier)
    }

    // -------------------------------------------------------------------------------------- what is rejected

    @Test
    void testValidateFile() {
        File real = new File(exchange, "real.xml")
        real.text = "<a/>"

        assertEquals([], submissionService.validateFile(real))
        assertTrue(submissionService.validateFile(new File(exchange, "missing.xml")).contains("File does not exist"))
        assertTrue(submissionService.validateFile(exchange).contains("The model file cannot be a directory"))
    }

    @Test
    void testValidateSyntaxAcceptsAValidModelAndRejectsInvalidOnes() {
        def validate = { String path, String format ->
            List<String> errors = []
            boolean valid = submissionService.validateSyntax(new File(path), format, errors)
            [valid: valid, errors: errors]
        }

        assertTrue(validate("jummp-plugins/jummp-plugin-sbml/test/files/Koch2017.xml", "SBML").valid)
        def invalidSbml = validate("test/files/invalidSbml.xml", "SBML")
        assertFalse(invalidSbml.valid)
        assertFalse(invalidSbml.errors.isEmpty())
        def invalidVersion = validate("test/files/invalidVersion.xml", "SBML")
        assertFalse(invalidVersion.valid)
        assertFalse(invalidVersion.errors.isEmpty())
        def invalidPharmml = validate("test/files/invalidPharmml.xml", "PharmML")
        assertFalse(invalidPharmml.valid)
        assertFalse(invalidPharmml.errors.isEmpty())
    }

    @Test
    void testValidateSyntaxOfAMissingFile() {
        List<String> errors = []

        assertFalse(submissionService.validateSyntax(new File(exchange, "missing.xml"), "SBML", errors))
        assertEquals(["Couldn't validate syntax on the file having physical errors"], errors)
    }

    @Test
    void testBuildingASubmissionWithoutFilesFails() {
        Map working = newWorking(stage([:]))
        String json = metadata([name: "No files", format: format("UNKNOWN"), files: [main: [], additional: []]])

        try {
            submissionService.buildFromJSONFile(json, working)
            fail("A submission without files must not be built")
        } catch (FileNotFoundException e) {
            assertTrue(e.message.contains("Cannot find the model files"))
        }
    }

    @Test
    void testBuildingASubmissionWithoutAModelNameFails() {
        String folder = stage(["mainFile.txt": "a main file"])
        Map working = newWorking(folder)
        String json = metadata([
            name: "", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]], additional: []]])

        try {
            submissionService.buildFromJSONFile(json, working)
            fail("A new model must have a name")
        } catch (IllegalAccessException e) {
            assertEquals("Cannot leave the model name and format properties empty!", working["cause"])
        }
    }

    @Test
    void testBuildingAnUpdateWithoutTheModelIdentifierFails() {
        String folder = stage(["mainFile.txt": "a main file"])
        Map working = newWorking(folder, true)
        String json = metadata([
            name: "Whatever", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]], additional: []]])

        try {
            submissionService.buildFromJSONFile(json, working)
            fail("An update must say which model it updates")
        } catch (IllegalAccessException e) {
            assertEquals("The model identifier is missing.", working["cause"])
        }
    }

    // ------------------------------------------------------------------------------ what is found in a file

    @Test
    void testDetectModelInfoOfAnSbmlModel() {
        Map info = submissionService.detectModelInfo(new File("jummp-plugins/jummp-plugin-sbml/test/files/Koch2017.xml"), "SBML")

        assertFalse(info.name.toString().isEmpty())
        assertTrue(info.containsKey("description"))
        assertTrue(info.containsKey("modellingApproach"))
    }

    @Test
    void testDetectModelInfoOfAFileNobodyKnows() {
        File unknown = new File(exchange, "unknown.txt")
        unknown.text = "just some text"

        Map info = submissionService.detectModelInfo(unknown, "UNKNOWN")

        assertEquals("", info.name)
        assertEquals("", info.description)
    }

    // ------------------------------------------------------------------------------------------ updates

    private String createWithAdditionalFile() {
        String folder = stage(["mainFile.txt": "the first main file", "addFile.txt": "the first additional file"])
        create(newWorking(folder), metadata([
            name: "A model to update", description: "It has two files", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]],
                    additional: [[name: "addFile.txt", description: "the additional file"]]]]))
    }

    /** What SubmissionController.makeSubmission does with the service for an update. Returns the working memory. */
    private Map update(String modelId, Map<String, String> files, List additional) {
        String folder = stage(files)
        Map working = newWorking(folder, true)
        submissionService.buildFromJSONFile(metadata([
            submissionId: modelId, name: "A model to update", description: "It has two files", format: format("UNKNOWN"),
            comment: "An update",
            files: [main: [[name: "mainFile.txt", description: "the main file"]], additional: additional]]), working)
        Set<String> changes = submissionService.handleSubmission(working)
        working.putAll([modelId: modelId, accessType: "update", changesMade: changes.sort()])
        submissionService.processPostSubmission(working)
        working
    }

    @Test
    void testUpdateModel() {
        String modelId = createWithAdditionalFile()

        update(modelId, ["mainFile.txt": "the second main file", "addFile.txt": "the second additional file"],
            [[name: "addFile.txt", description: "the additional file"]])

        Revision revision = latestRevisionOf(modelId)
        assertEquals(2, revision.revisionNumber)
        List<File> files = filesOf(modelId)
        assertEquals("the second main file", files.find { it.name == "mainFile.txt" }.text)
        assertEquals("the second additional file", files.find { it.name == "addFile.txt" }.text)
        // the update is recorded
        assertEquals(1, ModelAudit.countByModel(Model.findBySubmissionId(modelId)))
    }

    @Test
    void testAnUpdateWithoutAnAdditionalFileRemovesIt() {
        // JBM-764: a file the submitter leaves out of the update has to go, not to linger in the repository
        String modelId = createWithAdditionalFile()

        update(modelId, ["mainFile.txt": "the second main file"], [])

        assertEquals(2, latestRevisionOf(modelId).revisionNumber)
        assertEquals(["mainFile.txt"], filesOf(modelId)*.name)
    }

    // ------------------------------------------------------------------------------------------- cancel

    @Test
    void testCancellingRemovesTheStagedFilesAndCreatesNoModel() {
        String folder = stage(["mainFile.txt": "a main file", "addFile.txt": "an additional file"])
        Map working = newWorking(folder)
        submissionService.buildFromJSONFile(metadata([
            name: "Never submitted", format: format("UNKNOWN"),
            files: [main: [[name: "mainFile.txt", description: "the main file"]],
                    additional: [[name: "addFile.txt", description: "the additional file"]]]]), working)
        assertTrue(new File(exchange, folder).exists())

        submissionService.cleanup(working)

        assertFalse(new File(exchange, folder).exists())
        assertEquals(0, Model.count())
    }

    // ---------------------------------------------------------------------- how the web wizard starts a submission

    /** What ModelController.initialiseSubmission gives SubmissionService.initialise. */
    private Map initials(boolean isUpdate, String modelId = null) {
        Map<String, Object> initials = [isUpdate: isUpdate, isUpdateOnExistingModel: isUpdate,
                                        shouldCreateNewRevision: true] as HashMap<String, Object>
        if (isUpdate) {
            initials.put("modelId", modelId)
        }
        initials
    }

    /**
     * Runs a block with the FileSystemService using the exchange directory of the test. It gets the directory from
     * ConfigurationService, which reads the properties file, so it uses the developer's real one whatever the test has
     * put in the configuration.
     */
    private def withExchangeDirectoryOfTheFileSystemService(Closure body) {
        def original = fileSystemService.configurationService
        fileSystemService.configurationService = [loadVcsConfiguration: { ->
            new VcsCommand(vcs: "git", workingDirectory: "target/vcs/git", exchangeDirectory: exchange.path)
        }]
        try {
            return body()
        } finally {
            fileSystemService.configurationService = original
        }
    }

    @Test
    void testInitialiseANewSubmission() {
        Map working = initials(false)

        submissionService.initialise(working)

        // who is submitting
        assertEquals("[testuser, test@test.com]", working["submitterInfo"].toString())
        // a folder of its own, named after a UUID, for the files that are uploaded
        assertNotNull(UUID.fromString(working["submissionFolder"] as String))
        // the formats to choose from, by name, and the one to fall back on
        List<String> formatNames = working["sorted_model_formats"]*.name
        assertTrue(formatNames.contains("SBML"))
        assertEquals(formatNames.sort(false), formatNames)
        assertEquals(formatNames, working["modelFormatsSortedByName"]*.name)
        assertEquals("UNKNOWN", working["unknown_format_command"].identifier)
        // the modelling approaches to choose from (the one that setUp() creates)
        assertTrue(working["defined_modelling_approaches"]*.name.contains("Other"))
        assertEquals(working["defined_modelling_approaches"]*.name, working["definedModellingApproachNames"])
        // nothing has been submitted or entered yet
        assertEquals([], working["existingFiles"])
        assertTrue(working.containsKey("publication"))
        assertNull(working["publication"])
        assertEquals("", working["modellingApproach"])
        assertEquals("", working["otherInfo"])
        assertEquals("", working["readmeSubmission"])
        assertFalse(working["isMetadataSubmission"])
        // one place for the details of a publication of each kind, none of them from the database yet
        Map publications = working["publication_objects_in_working"]
        assertEquals(PublicationLinkProvider.LinkType.values()*.label as Set, publications.keySet() as Set)
        assertTrue(publications.values().every { !it.comesFromDatabase })
        assertSame(publications, working["publicationContext"])
        // the style sheets of the wizard's pages
        assertTrue(working["submissionCssHref"].toString().endsWith("submission.css"))
    }

    @Test
    void testEveryNewSubmissionGetsAFolderOfItsOwn() {
        Map first = initials(false)
        Map second = initials(false)

        submissionService.initialise(first)
        submissionService.initialise(second)

        assertNotNull(first["submissionFolder"])
        assertNotNull(second["submissionFolder"])
        assertFalse(first["submissionFolder"] == second["submissionFolder"])
    }

    @Test
    void testInitialiseAnUpdateOfAModel() {
        String modelId = createWithAdditionalFile()
        Map working = initials(true, modelId)

        withExchangeDirectoryOfTheFileSystemService { submissionService.initialise(working) }

        // what the model is now
        assertEquals(1, working["RevisionNumber"])
        assertEquals("A model to update", working["latestModelName"])
        assertEquals("It has two files", working["latestModelDescription"])
        ModelFormat unknown = ModelFormat.findByIdentifier("UNKNOWN")
        assertEquals(unknown.id, working["latestModelFormat"])
        assertEquals("${unknown.name} ${unknown.formatVersion}".toString(), working["latestModelFormatNameAndVersion"].toString())
        assertFalse(working["isMetadataSubmission"])
        // the first revision of a model can never be amended
        assertFalse(working["amendable"])
        // its files, the main file first
        List<Map> files = working["existingFilesList"]
        assertEquals(["mainFile.txt", "addFile.txt"], files*.filename)
        assertEquals([true, false], files*.isModelFile)
        assertEquals(["the main file", "the additional file"], files*.description)
        // and a copy of them in the submission folder, for the submitter to change
        File folder = new File(exchange, working["submissionFolder"] as String)
        assertEquals(["addFile.txt", "mainFile.txt"], folder.list().sort() as List)
        assertEquals("the first main file", new File(folder, "mainFile.txt").text)
        assertEquals("the first additional file", new File(folder, "addFile.txt").text)
    }

    @Test
    void testInitialiseAnUpdateOfALaterRevision() {
        String modelId = createWithAdditionalFile()
        update(modelId, ["mainFile.txt": "the second main file", "addFile.txt": "the second additional file"],
            [[name: "addFile.txt", description: "the additional file"]])
        Map working = initials(true, modelId)

        withExchangeDirectoryOfTheFileSystemService { submissionService.initialise(working) }

        assertEquals(2, working["RevisionNumber"])
        // a revision after the first can be amended
        assertTrue(working["amendable"])
        // the files are those of the latest revision
        File folder = new File(exchange, working["submissionFolder"] as String)
        assertEquals("the second main file", new File(folder, "mainFile.txt").text)
        assertEquals("the second additional file", new File(folder, "addFile.txt").text)
    }
}
