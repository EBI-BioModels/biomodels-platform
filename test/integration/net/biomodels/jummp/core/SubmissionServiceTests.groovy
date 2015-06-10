/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* JGit, Apache Commons, JUnit, Spring Security (or a modified version of that library), containing parts
* covered by the terms of Common Public License, Apache License v2.0, Eclipse Distribution License v1.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of JGit, Apache Commons, JUnit, Spring Security used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import static org.junit.Assert.*
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import java.util.UUID
import net.biomodels.jummp.core.adapters.DomainAdapter 
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.git.GitManagerFactory
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.User
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.*
import org.springframework.security.acls.domain.BasePermission

@TestMixin(IntegrationTestMixin)
class SubmissionServiceTests extends JummpIntegrationTest {
    /**
     * Dependency Injection of Grails Application
     */
    def grailsApplication
    /**
     * Dependency Injection of SubmissionService
     */
    def submissionService
    /**
    * Dependency injection of modelService
    */
    def modelService
    /**
     * Dependency injection of fileSystemService
     */
    def fileSystemService

    @Before
    void setUp() {
        // Setup logic here
        createUserAndRoles()
        setupVcs()
    }

    @After
    void tearDown() {
        // Tear down logic here
        FileUtils.deleteDirectory(new File("target/vcs/git"))
        FileUtils.deleteDirectory(new File("target/vcs/exchange"))
        modelService.vcsService.vcsManager = null
    }

    // TODO: remove this copy from JmsAdapterServiceTest
    private void setupVcs() {
        File root = new File("target/vcs/git")
        assertTrue root.mkdirs()
        assertTrue new File("target/vcs/exchange/").mkdirs()
        fileSystemService.root = root
        String containerPath = root.absolutePath + "/aaa/"
        fileSystemService.currentModelContainer = containerPath
        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        grailsApplication.config.jummp.vcs.workingDirectory = "target/vcs/git"
        grailsApplication.config.jummp.vcs.exchangeDirectory = "target/vcs/exchange"
        modelService.vcsService.vcsManager = gitService.getInstance()
        modelService.vcsService.modelContainerRoot = root.absolutePath
        assertTrue(modelService.vcsService.isValid())
    }

    private String addTestFile(Map<String, Object> workingMemory) {
        String guid = UUID.randomUUID()
        def mains = [getFile("mainFile.txt", "this is a main file")]
        File additional = getFile("addFile.txt", guid)
        Map<File, String> adds=new HashMap<File, String>()
        adds.put(additional, "this is a desc")
        workingMemory.put("submitted_mains", mains)
        workingMemory.put("submitted_additionals", adds)
        submissionService.handleFileUpload(workingMemory)
        assertTrue(workingMemory.containsKey("repository_files"))
        assertTrue(workingMemory.get("repository_files").size() == 2)
        submissionService.inferModelFormatType(workingMemory)
        assertTrue(workingMemory.containsKey("model_type"))
        assertEquals(workingMemory.get("model_type").identifier, "UNKNOWN")
        submissionService.performValidation(workingMemory)
        assertTrue(workingMemory.containsKey("model_validation_result"))
        assertEquals(workingMemory.get("model_validation_result") as Boolean, true)
        workingMemory.put("Valid", true)
        submissionService.inferModelInfo(workingMemory)
        assertTrue(workingMemory.containsKey("RevisionTC"))
        return guid
    }

    private String addAdditionalFile(Map<String, Object> workingMemory, String directory=null) {
        String guid = UUID.randomUUID()
        File additional
        if (!directory) {
            additional = getFile("addFile.txt", guid)
        }
        else {
            additional = getFile("addFile.txt", guid, directory)
        }
        Map<File, String> adds = new HashMap<File, String>()
        adds.put(additional, "this is a desc")
        workingMemory.put("submitted_mains", [])
        workingMemory.put("submitted_additionals", adds)
        submissionService.handleFileUpload(workingMemory)
        assertTrue(workingMemory.containsKey("repository_files"))
        assertTrue(workingMemory.get("repository_files").size() == 2)
        submissionService.inferModelFormatType(workingMemory)
        assertTrue(workingMemory.containsKey("model_type"))
        assertEquals(workingMemory.get("model_type").identifier, "UNKNOWN")
        submissionService.performValidation(workingMemory)
        assertTrue(workingMemory.containsKey("model_validation_result"))
        assertEquals(workingMemory.get("model_validation_result") as Boolean, true)
        workingMemory.put("Valid", true)
        submissionService.inferModelInfo(workingMemory)
        assertTrue(workingMemory.containsKey("RevisionTC"))
        return guid
    }


    private void deleteAdditionalFile(Map<String, Object> workingMemory) {
        def deleteThese = ["addFile.txt"]
        workingMemory.put("deleted_filenames", deleteThese)
        submissionService.handleFileUpload(workingMemory)
        assertTrue(workingMemory.containsKey("repository_files"))
        assertTrue(workingMemory.get("repository_files").size() == 1)
        submissionService.inferModelFormatType(workingMemory)
        assertTrue(workingMemory.containsKey("model_type"))
        assertEquals(workingMemory.get("model_type").identifier, "UNKNOWN")
        submissionService.performValidation(workingMemory)
        assertTrue(workingMemory.containsKey("model_validation_result"))
        assertEquals(workingMemory.get("model_validation_result") as Boolean, true)
        workingMemory.put("Valid", true)
        submissionService.inferModelInfo(workingMemory)
        assertTrue(workingMemory.containsKey("RevisionTC"))
    }

    private void confirmFile(String modelID, String confirmationText,
                String fileName = "addFile.txt") {
        List<File> files = modelService.retrieveModelRepFiles(
                modelService.getLatestRevision(modelService.getModel(modelID)))
        if (!confirmationText) { //no confirmation text means the file shouldnt exist
            assertNull(files.find { it.getName() == fileName})
        }
        else {
            File testFile = files.find { it.getName() == fileName }
            String fileText = testFile.getText()
            assertEquals(fileText, confirmationText)
            assertFalse(fileText == "not this text")
        }
    }

    // Creates a model and submits it
    @Test
    void testModelSubmission() {
        assertNotNull(authenticateAsTestUser())
        Map<String, Object> workingMemory = new HashMap<String, Object>()
        workingMemory.put("isUpdateOnExistingModel", false)
        submissionService.initialise(workingMemory)
        String testThis = addTestFile(workingMemory)
        def revSummary = ['RevisionComments':'Model revised without commit message']
        submissionService.updateFromSummary(workingMemory, revSummary)
        submissionService.handleSubmission(workingMemory)
        assertTrue(workingMemory.containsKey("model_id"))
        confirmFile(workingMemory.get("model_id") as String, testThis)
    }

    // Creates a model with a delete after upload
    @Test
    void testModelSubmissionWithDelete() {
        assertNotNull(authenticateAsTestUser())
        Map<String, Object> workingMemory = new HashMap<String, Object>()
        workingMemory.put("isUpdateOnExistingModel", false)
        submissionService.initialise(workingMemory)
        String testThis = addTestFile(workingMemory)
        // delete the additional file just uploaded
        deleteAdditionalFile(workingMemory)
        def revSummary = ['RevisionComments':'Model revised without commit message']
        submissionService.updateFromSummary(workingMemory, revSummary)
        submissionService.handleSubmission(workingMemory)
        assertTrue(workingMemory.containsKey("model_id"))
        confirmFile(workingMemory.get("model_id") as String, null)
    }

    // Updates a file, tests whether the text is the updated text
    @Test
    void testModelUpdate() {
        Map<String, Object> workingMemory = new HashMap<String, Object>()
        assertNotNull(authenticateAsTestUser())
        workingMemory.put("isUpdateOnExistingModel", false)
        submissionService.initialise(workingMemory)
        String testThis = addTestFile(workingMemory)
        def revSummary = ['RevisionComments':'Model revised without commit message']
        submissionService.updateFromSummary(workingMemory, revSummary)
        submissionService.handleSubmission(workingMemory)
        assertTrue(workingMemory.containsKey("model_id"))
        String model_id = workingMemory.get("model_id")
        confirmFile("$model_id", testThis)
        // model created
        workingMemory = new HashMap<String, Object>()
        workingMemory.put("isUpdateOnExistingModel", true)
        workingMemory.put("model_id", model_id)
        workingMemory.put("LastRevision", DomainAdapter.getAdapter(modelService
                                                                .getLatestRevision(modelService
                                                                .getModel(model_id))).toCommandObject())
        submissionService.initialise(workingMemory)
        String directory = new File(workingMemory.get("LastRevision").getFiles()[0].path).getParent()
        String text = addAdditionalFile(workingMemory, directory)
        submissionService.updateFromSummary(workingMemory, revSummary)
        submissionService.handleSubmission(workingMemory)
        //model updated... test that the text is what it should be
        confirmFile(workingMemory.get("model_id") as String, text)
    }

    // Updates a file, tests whether the text is the updated text
    @Test
    void testModelUpdateReplaceMainFile() {
        new File("target/vcs/exchange/").mkdirs()
        Map<String, Object> workingMemory = new HashMap<String, Object>()
        assertNotNull(authenticateAsTestUser())
        workingMemory.put("isUpdateOnExistingModel", false)
        submissionService.initialise(workingMemory)
        String testThis = addTestFile(workingMemory)
        def revSummary = ['RevisionComments':'Model revised without commit message']
        submissionService.updateFromSummary(workingMemory, revSummary)
        submissionService.handleSubmission(workingMemory)
        assertTrue(workingMemory.containsKey("model_id"))
        String model_id = workingMemory.get("model_id")
        confirmFile(model_id, testThis)
        // model created
        workingMemory = new HashMap<String, Object>()
        workingMemory.put("isUpdateOnExistingModel", true)
        workingMemory.put("model_id", model_id)
        workingMemory.put("LastRevision", DomainAdapter
                                            .getAdapter(modelService.getLatestRevision(modelService.getModel(model_id)))
                                            .toCommandObject())
        submissionService.initialise(workingMemory)
        String directory = new File(workingMemory.get("LastRevision").getFiles()[0].path).getParent()
        String guid = UUID.randomUUID() as String
        File newMain = getFile("newMain.txt", guid, directory)
        workingMemory.put("submitted_mains", [newMain])
        def deleteThese = ["mainFile.txt"]
        workingMemory.put("deleted_filenames", deleteThese)
        submissionService.handleFileUpload(workingMemory)
        assertTrue(workingMemory.containsKey("repository_files"))
        assertTrue(workingMemory.get("repository_files").size() == 2)
        submissionService.inferModelFormatType(workingMemory)
        assertTrue(workingMemory.containsKey("model_type"))
        assertEquals(workingMemory.get("model_type").identifier, "UNKNOWN")
        submissionService.performValidation(workingMemory)
        assertTrue(workingMemory.containsKey("model_validation_result"))
        assertEquals(workingMemory.get("model_validation_result") as Boolean, true)
        workingMemory.put("Valid", true)
        submissionService.inferModelInfo(workingMemory)
        assertTrue(workingMemory.containsKey("RevisionTC"))
        submissionService.updateFromSummary(workingMemory, revSummary)
        submissionService.handleSubmission(workingMemory)
        //model updated... test that the text is what it should be
        confirmFile(workingMemory.get("model_id") as String, guid, "newMain.txt")
    }

    //Creates a model, updates it, with a file first updated then deleted. Should be a delete
    @Test
    void testModelUpdateThenDelete() {
        Map<String, Object> workingMemory = new HashMap<String, Object>()
        assertNotNull(authenticateAsTestUser())
        workingMemory.put("isUpdateOnExistingModel", false)
        submissionService.initialise(workingMemory)
        String testThis = addTestFile(workingMemory)
        def revSummary = ['RevisionComments':'Model revised without commit message']
        submissionService.updateFromSummary(workingMemory, revSummary)
        submissionService.handleSubmission(workingMemory)
        assertTrue(workingMemory.containsKey("model_id"))
        String model_id = workingMemory.get("model_id")
        confirmFile(""+model_id, testThis)
        // model created
        workingMemory = new HashMap<String, Object>()
        workingMemory.put("isUpdateOnExistingModel", true)
        workingMemory.put("model_id", model_id)
        workingMemory.put("LastRevision", DomainAdapter
                                            .getAdapter(modelService.getLatestRevision(modelService.getModel(model_id)))
                                            .toCommandObject())
        submissionService.initialise(workingMemory)
        String directory = new File(workingMemory.get("LastRevision").getFiles()[0].path).getParent()
        String text = addAdditionalFile(workingMemory, directory)
        // delete the file now
        def deleteThese = ["addFile.txt"]
        workingMemory.put("deleted_filenames", deleteThese)
        submissionService.handleFileUpload(workingMemory)
        assertTrue(workingMemory.containsKey("repository_files"))
        assertTrue(workingMemory.get("repository_files").size() == 1)
        submissionService.inferModelFormatType(workingMemory)
        assertTrue(workingMemory.containsKey("model_type"))
        assertEquals(workingMemory.get("model_type").identifier, "UNKNOWN")
        submissionService.performValidation(workingMemory)
        assertTrue(workingMemory.containsKey("model_validation_result"))
        assertEquals(workingMemory.get("model_validation_result") as Boolean, true)
        workingMemory.put("Valid", true)
        submissionService.inferModelInfo(workingMemory)
        assertTrue(workingMemory.containsKey("RevisionTC"))

        // complete update, and test that the file does not exist in repository
        submissionService.updateFromSummary(workingMemory, revSummary)
        submissionService.handleSubmission(workingMemory)
        confirmFile(workingMemory.get("model_id") as String, null)
    }

    //Creates a model, updates it, with a file first deleted then updated. Should be an update
    @Test
    void testModelDeleteThenUpdate() {
        Map<String, Object> workingMemory = new HashMap<String, Object>()
        assertNotNull(authenticateAsTestUser())
        workingMemory.put("isUpdateOnExistingModel", false)
        submissionService.initialise(workingMemory)
        String testThis = addTestFile(workingMemory)
        def revSummary = ['RevisionComments':'Model revised without commit message']
        submissionService.updateFromSummary(workingMemory, revSummary)
        submissionService.handleSubmission(workingMemory)
        assertTrue(workingMemory.containsKey("model_id"))
        String model_id = workingMemory.get("model_id")
        confirmFile(model_id, testThis)
        // model created
        workingMemory = new HashMap<String, Object>()
        workingMemory.put("isUpdateOnExistingModel", true)
        workingMemory.put("model_id", model_id)
        workingMemory.put("LastRevision", DomainAdapter
                                            .getAdapter(modelService.getLatestRevision(modelService.getModel(model_id)))
                                            .toCommandObject())
        submissionService.initialise(workingMemory)
        def deleteThese = ["addFile.txt"]
        workingMemory.put("deleted_filenames", deleteThese)
        submissionService.handleFileUpload(workingMemory)
        assertTrue(workingMemory.containsKey("repository_files"))
        assertTrue(workingMemory.get("repository_files").size() == 1)
        // file deleted
        submissionService.inferModelFormatType(workingMemory)
        assertTrue(workingMemory.containsKey("model_type"))
        assertEquals(workingMemory.get("model_type").identifier, "UNKNOWN")
        submissionService.performValidation(workingMemory)
        assertTrue(workingMemory.containsKey("model_validation_result"))
        assertEquals(workingMemory.get("model_validation_result") as Boolean, true)
        workingMemory.put("Valid", true)
        submissionService.inferModelInfo(workingMemory)
        assertTrue(workingMemory.containsKey("RevisionTC"))
        // add the file again
        String directory = new File(workingMemory.get("LastRevision").getFiles()[0].path).getParent()
        String text = addAdditionalFile(workingMemory, directory)
        submissionService.handleFileUpload(workingMemory)
        assertTrue(workingMemory.containsKey("repository_files"))
        assertTrue(workingMemory.get("repository_files").size() == 2)
        submissionService.inferModelFormatType(workingMemory)
        assertTrue(workingMemory.containsKey("model_type"))
        assertEquals(workingMemory.get("model_type").identifier, "UNKNOWN")
        submissionService.performValidation(workingMemory)
        assertTrue(workingMemory.containsKey("model_validation_result"))
        assertEquals(workingMemory.get("model_validation_result") as Boolean, true)
        workingMemory.put("Valid", true)
        submissionService.inferModelInfo(workingMemory)
        assertTrue(workingMemory.containsKey("RevisionTC"))
        // submit and ensure that the text is the new text
        submissionService.updateFromSummary(workingMemory, revSummary)
        submissionService.handleSubmission(workingMemory)
        confirmFile(workingMemory.get("model_id") as String, text)
    }

    @Test
    void testModelDelete() {
        Map<String, Object> workingMemory = new HashMap<String, Object>()
        assertNotNull(authenticateAsTestUser())
        workingMemory.put("isUpdateOnExistingModel", false)
        submissionService.initialise(workingMemory)
        String testThis = addTestFile(workingMemory)
        def revSummary = ['RevisionComments':'Model revised without commit message']
        submissionService.updateFromSummary(workingMemory, revSummary)
        submissionService.handleSubmission(workingMemory)
        assertTrue(workingMemory.containsKey("model_id"))
        String model_id = workingMemory.get("model_id")
        confirmFile(model_id, testThis)
        workingMemory = new HashMap<String, Object>()
        workingMemory.put("isUpdateOnExistingModel", true)
        workingMemory.put("model_id", model_id)
        workingMemory.put("LastRevision", DomainAdapter
                                            .getAdapter(modelService.getLatestRevision(modelService.getModel(model_id)))
                                            .toCommandObject())
        submissionService.initialise(workingMemory)
        def deleteThese = ["addFile.txt"]
        workingMemory.put("deleted_filenames", deleteThese)
        submissionService.handleFileUpload(workingMemory)
        assertTrue(workingMemory.containsKey("repository_files"))
        assertTrue(workingMemory.get("repository_files").size() == 1)
        submissionService.inferModelFormatType(workingMemory)
        assertTrue(workingMemory.containsKey("model_type"))
        assertEquals(workingMemory.get("model_type").identifier, "UNKNOWN")
        submissionService.performValidation(workingMemory)
        assertTrue(workingMemory.containsKey("model_validation_result"))
        assertEquals(workingMemory.get("model_validation_result") as Boolean, true)
        workingMemory.put("Valid", true)
        submissionService.inferModelInfo(workingMemory)
        assertTrue(workingMemory.containsKey("RevisionTC"))
        submissionService.updateFromSummary(workingMemory, revSummary)
        submissionService.handleSubmission(workingMemory)
        confirmFile(workingMemory.get("model_id") as String, null)
    }

    protected File getFile(String filename, String text,
                String directory=grailsApplication.config.jummp.vcs.exchangeDirectory) {
        File testFile = new File(new File(directory), filename)
        testFile.setText(text)
        return testFile
    }
    
}
