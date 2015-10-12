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

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
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
import static org.junit.Assert.*

@TestMixin(IntegrationTestMixin)
class ModelHistoryServiceTests extends JummpIntegrationTest {
    /**
     * Dependency Injection of Grails Application
     */
    def grailsApplication
    /**
     * Dependency Injection of Model Service
     */
    def modelService
    /**
     * Dependency Injection of Model History Service
     */
    def modelHistoryService
    /**
     * Dependency Injection of ACL Util Service
     */
    def aclUtilService

    @Before
    void setUp() {
        // Setup logic here
        createUserAndRoles()
        setupVcs()
        createModels()
    }

    @After
    void tearDown() {
        // Tear down logic here
        FileUtils.deleteDirectory(new File("target/vcs/git"))
        FileUtils.deleteDirectory(new File("target/vcs/exchange"))
        modelService.vcsService.vcsManager = null
    }

    @Test
    void testNotAuthorizedUser() {
        authenticateAnonymous()
        // test with disabled history functionality
        grailsApplication.config.jummp.model.history.maxElements = 0
        // ensure that the history is empty
        assertTrue(modelHistoryService.history().isEmpty())
        assertNull(modelHistoryService.lastAccessedModel().id)
        // add a Model to the history by accessing its revision
        assertNotNull(modelService.getRevision(Revision.findByName("model1").model, 1))
        // everything should still be empty
        assertTrue(modelHistoryService.history().isEmpty())
        assertNull(modelHistoryService.lastAccessedModel().id)
        // just ensure that this does not change when adding the history directly
        modelHistoryService.addModelToHistory(Revision.findByName("model1").model)
        assertTrue(modelHistoryService.history().isEmpty())
        assertNull(modelHistoryService.lastAccessedModel().id)

        // now the same with history enabled
        grailsApplication.config.jummp.model.history.maxElements = 10
        // add a Model to the history by accessing its revision
        assertNotNull(modelService.getRevision(Revision.findByName("model1").model, 1))
        // everything should still be empty
        assertTrue(modelHistoryService.history().isEmpty())
        assertNull(modelHistoryService.lastAccessedModel().id)
        // just ensure that this does not change when adding the history directly
        modelHistoryService.addModelToHistory(Revision.findByName("model1").model)
        assertTrue(modelHistoryService.history().isEmpty())
        assertNull(modelHistoryService.lastAccessedModel().id)

        // and just for the joy of it - trying all Models
        (1..11).each { it ->
            assertNotNull(modelService.getRevision(Revision.findByName("model${it}").model, 1))
            assertTrue(modelHistoryService.history().isEmpty())
            assertNull(modelHistoryService.lastAccessedModel().id)
        }
    }

    @Test
    void testAuthorizedUser() {
        authenticateAsTestUser()
        // test with disabled history functionality
        grailsApplication.config.jummp.model.history.maxElements = 0
        // ensure that the history is empty
        assertTrue(modelHistoryService.history().isEmpty())
        assertNull(modelHistoryService.lastAccessedModel().id)
        // add a Model to the history by accessing its revision
        assertNotNull(modelService.getRevision(Revision.findByName("model1").model, 1))
        // everything should still be empty
        assertTrue(modelHistoryService.history().isEmpty())
        assertNull(modelHistoryService.lastAccessedModel().id)
        // just ensure that this does not change when adding the history directly
        modelHistoryService.addModelToHistory(Revision.findByName("model1").model)
        assertTrue(modelHistoryService.history().isEmpty())
        assertNull(modelHistoryService.lastAccessedModel().id)

        // now the same with history enabled
        grailsApplication.config.jummp.model.history.maxElements = 10
        assertTrue(modelHistoryService.history().isEmpty())
        // add a Model to the history by accessing its revision
        Model model = Revision.findByName("model1").model
        assertNotNull(modelService.getRevision(model, 1))
        // the model should now be visible in the history
        List<ModelTransportCommand> history = modelHistoryService.history()
        assertFalse(history.isEmpty())
        assertEquals(1, history.size())
        assertEquals(model.id, history.first().id)
        assertEquals(model.id, modelHistoryService.lastAccessedModel().id)
        // accessing the same Model again should not change anything
        assertNotNull(modelService.getRevision(model, 1))
        history = modelHistoryService.history()
        assertFalse(history.isEmpty())
        assertEquals(1, history.size())
        assertEquals(model.id, history.first().id)
        assertEquals(model.id, modelHistoryService.lastAccessedModel().id)

        long idOfPreviousModel = model.id
        (2..10).each {
            Model currentModel = Revision.findByName("model${it}").model
            assertNotNull(modelService.getRevision(currentModel, 1))
            history = modelHistoryService.history()
            assertEquals(it, history.size())
            // the current model has to be the first one
            assertEquals(currentModel.id, history.first().id)
            assertEquals(currentModel.id, modelHistoryService.lastAccessedModel().id)
            idOfPreviousModel = currentModel.id
        }
        // now the size is 10
        assertEquals(10, modelHistoryService.history().size())
        // access one item to get it to the front
        Model currentModel = Revision.findByName("model5").model
        assertNotNull(modelService.getRevision(currentModel, 1))
        history = modelHistoryService.history()
        ModelTransportCommand lastModel = modelHistoryService.lastAccessedModel()
        assertEquals(10, history.size())
        assertEquals(currentModel.id, history.first().id)
        assertEquals(currentModel.id, lastModel.id)
        assertEquals(Revision.findByName("model10").model.id, history[1].id)

        // accessing a Model not yet in the list should throw out the oldest
        currentModel = Revision.findByName("model11").model
        assertNotNull(modelService.getRevision(currentModel, 1))
        history = modelHistoryService.history()
        assertEquals(10, history.size())
        assertEquals(currentModel.id, history.first().id)

        assertEquals(currentModel.id, modelHistoryService.lastAccessedModel().id)
        assertEquals(Revision.findByName("model2").model.id, history.last().id)
        history.each {
            assertTrue(model.id != it.id)
        }

        // turn the feature off again
        grailsApplication.config.jummp.model.history.maxElements = 0
        assertTrue(modelHistoryService.history().isEmpty())
        assertNull(modelHistoryService.lastAccessedModel().id)
        // accessing a Model should not change it
        assertNotNull(modelService.getRevision(currentModel, 1))
        assertTrue(modelHistoryService.history().isEmpty())
        assertNull(modelHistoryService.lastAccessedModel().id)
    }

    // TODO: remove this copy from JmsAdapterServiceTest
    private void setupVcs() {
        File root = new File("target/vcs/git/hhh")
        root.mkdirs()
        String containerPath = root.absolutePath
        modelService.fileSystemService.root = root.parentFile
        File exchangeDir = new File("target/vcs/exchange")
        exchangeDir.mkdirs()
        assertTrue exchangeDir.exists()
        grailsApplication.config.jummp.vcs.workingDirectory = root.parent
        grailsApplication.config.jummp.vcs.exchangeDirectory = exchangeDir.path
        modelService.fileSystemService.currentModelContainer = containerPath
        modelService.vcsService.modelContainerRoot = root.parentFile
        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        modelService.vcsService.vcsManager = gitService.getInstance()
        modelService.vcsService.vcsManager.exchangeDirectory = exchangeDir
        assertTrue(modelService.vcsService.isValid())
    }

    /**
     * Helper method to create 11 Models to test with
     */
    private void createModels() {
        authenticateAsTestUser()
        grailsApplication.config.jummp.model.history.maxElements = 0
        // try uploading a valid model
        String modelSource = '''<?xml version="1.0" encoding="UTF-8"?>
<sbml xmlns="http://www.sbml.org/sbml/level1" level="1" version="1">
  <model>
    <listOfCompartments>
      <compartment name="x"/>
    </listOfCompartments>
    <listOfSpecies>
      <specie name="y" compartment="x" initialAmount="1"/>
    </listOfSpecies>
    <listOfReactions>
      <reaction name="r">
        <listOfReactants>
          <specieReference specie="y"/>
        </listOfReactants>
        <listOfProducts>
          <specieReference specie="y"/>
        </listOfProducts>
      </reaction>
    </listOfReactions>
  </model>
</sbml>'''
        File file = File.createTempFile("jummpJms", null)
        def rf = new RepositoryFileTransportCommand(path: file.absolutePath, description: "", mainFile: true)
        file.append(modelSource)
        Model model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(comment: "Test Comment",
                name: "model1", format: new ModelFormatTransportCommand(identifier: "SBML"),
                submissionId: "M1"))
        aclUtilService.addPermission(modelService.getLatestRevision(model), "ROLE_ANONYMOUS", BasePermission.READ)
        model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(comment: "Test Comment",
                name: "model2", format: new ModelFormatTransportCommand(identifier: "SBML"),
                submissionId: "M2"))
        aclUtilService.addPermission(modelService.getLatestRevision(model), "ROLE_ANONYMOUS", BasePermission.READ)
        model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(comment: "Test Comment",
                name: "model3", format: new ModelFormatTransportCommand(identifier: "SBML"),
                submissionId: "M3"))
        aclUtilService.addPermission(modelService.getLatestRevision(model), "ROLE_ANONYMOUS", BasePermission.READ)
        model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(comment: "Test Comment",
                name: "model4", format: new ModelFormatTransportCommand(identifier: "SBML"),
                submissionId: "M4"))
        aclUtilService.addPermission(modelService.getLatestRevision(model), "ROLE_ANONYMOUS", BasePermission.READ)
        model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(comment: "Test Comment",
                name: "model5", format: new ModelFormatTransportCommand(identifier: "SBML"),
                submissionId: "M5"))
        aclUtilService.addPermission(modelService.getLatestRevision(model), "ROLE_ANONYMOUS", BasePermission.READ)
        model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(comment: "Test Comment",
                name: "model6", format: new ModelFormatTransportCommand(identifier: "SBML"),
                submissionId: "M6"))
        aclUtilService.addPermission(modelService.getLatestRevision(model), "ROLE_ANONYMOUS", BasePermission.READ)
        model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(comment: "Test Comment",
                name: "model7", format: new ModelFormatTransportCommand(identifier: "SBML"),
                submissionId: "M7"))
        aclUtilService.addPermission(modelService.getLatestRevision(model), "ROLE_ANONYMOUS", BasePermission.READ)
        model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(comment: "Test Comment",
                name: "model8", format: new ModelFormatTransportCommand(identifier: "SBML"),
                submissionId: "M8"))
        aclUtilService.addPermission(modelService.getLatestRevision(model), "ROLE_ANONYMOUS", BasePermission.READ)
        model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(comment: "Test Comment",
                name: "model9", format: new ModelFormatTransportCommand(identifier: "SBML"),
                submissionId: "M9"))
        aclUtilService.addPermission(modelService.getLatestRevision(model), "ROLE_ANONYMOUS", BasePermission.READ)
        model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(comment: "Test Comment",
                name: "model10", format: new ModelFormatTransportCommand(identifier: "SBML"),
                submissionId: "M10"))
        aclUtilService.addPermission(modelService.getLatestRevision(model), "ROLE_ANONYMOUS", BasePermission.READ)
        model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(comment: "Test Comment",
                name: "model11", format: new ModelFormatTransportCommand(identifier: "SBML"),
                submissionId: "M11"))
        aclUtilService.addPermission(modelService.getLatestRevision(model), "ROLE_ANONYMOUS", BasePermission.READ)
        FileUtils.deleteQuietly(file)
    }
}
