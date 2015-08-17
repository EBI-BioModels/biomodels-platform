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
 * Apache Commons, JUnit (or a modified version of that library), containing parts
 * covered by the terms of Common Public License, Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Apache Commons, JUnit used as well as
 * that of the covered work.}
 **/

package net.biomodels.jummp.core

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.plugins.git.GitManagerFactory
import org.apache.commons.io.FileUtils
import org.junit.*
import static org.junit.Assert.*

@TestMixin(IntegrationTestMixin)
class SearchTests extends JummpIntegrationTest {
    def searchService
    def modelService
    def fileSystemService
    def grailsApplication
    def solrServerHolder

    @Test
    void testGetLatestRevision() {
        // generate unique ids for the name and description
        String nameTag = "testModel"
        String descriptionTag = "test description"
        authenticateAsTestUser()
        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        grailsApplication.config.jummp.vcs.exchangeDirectory = "target/vcs/exchange"
        grailsApplication.config.jummp.vcs.workingDirectory = "target/vcs/git"
        grailsApplication.config.jummp.plugins.sbml.validation = false
        modelService.vcsService.vcsManager = gitService.getInstance()
        modelService.vcsService.vcsManager.exchangeDirectory = new File('target/vcs/exchange')
        // upload the model
        def rf = new RepositoryFileTransportCommand(path:
                smallModel("importModel.xml", nameTag, descriptionTag).absolutePath,
                mainFile: true, description: "")
        Model upped = modelService.uploadModelAsFile(rf, new ModelTransportCommand(format:
                new ModelFormatTransportCommand(identifier: "PharmML"), comment: "test", name: "Test"))
        //wait a bit for the model to be indexed
        Thread.sleep(25000)
        // Search for the model using the name and description, and ensure it's the same we uploaded
        ModelTransportCommand result = searchForModel(nameTag)
        assertNotNull result
        assertEquals(upped.id, result.id)
        result = searchForModel(descriptionTag)
        assertNotNull result
        assertEquals(upped.id, result.id)
        result = searchForModel(upped.submissionId)
        assertNotNull result
        assertEquals(upped.id, result.id)
        result = searchForModel("submissionId:${upped.submissionId}")
        assertNotNull result
        assertEquals(upped.id, result.id)
        result = searchForModel("pharmml")
        assertNotNull result
        assertEquals(upped.id, result.id)
    }

    @Before
    void setUp() {
        def container = new File("target/vcs/git/ggg/")
        container.mkdirs()
        new File("target/vcs/exchange/").mkdirs()
        fileSystemService.currentModelContainer = container.getCanonicalPath()
        fileSystemService.root = container.getParentFile()
        modelService.vcsService.modelContainerRoot = fileSystemService.root
        createUserAndRoles()
        assertNotNull solrServerHolder.server
    }

    @After
     void tearDown() {
        try {
            FileUtils.deleteDirectory(new File("target/vcs/git"))
            FileUtils.deleteDirectory(new File("target/vcs/exchange"))
        } catch(Exception ignore) {
        }
        modelService.vcsService.vcsManager = null
        solrServerHolder.server.deleteByQuery("*:*")
        solrServerHolder.server.commit()
    }

    ModelTransportCommand searchForModel(String query) {
        Collection<ModelTransportCommand> mods = searchService.searchModels(query)
        if (mods.isEmpty()) {
            return null
        }
        return mods.first()
    }

    // Convenience functions follow..
    private File smallModel(String filename, String id, String desc) {
        return new File("test/files/test.xml")
    }

    private File getFileForTest(String filename, String text) {
        def tempDir = FileUtils.getTempDirectory()
        def testFile = new File(tempDir.absolutePath + File.separator + filename)
        if (text) {
            testFile.setText(text)
        }
        return testFile
    }
}
