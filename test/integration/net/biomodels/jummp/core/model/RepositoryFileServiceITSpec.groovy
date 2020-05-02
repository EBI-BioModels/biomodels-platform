/**
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 */





package net.biomodels.jummp.core.model

import grails.test.runtime.FreshRuntime
import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.core.JummpIntegrationTest
import net.biomodels.jummp.core.ModelException
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.core.util.JummpXmlUtils
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.User
import org.apache.commons.io.FileUtils
import org.springframework.transaction.TransactionDefinition

import java.nio.file.Path
import java.nio.file.Paths

@FreshRuntime
class RepositoryFileServiceITSpec extends IntegrationSpec {
    def fileSystemService
    def grailsApplication
    def jummpIntegrationTest
    def miriamService
    def modelDelegateService
    def modelService
    def pubMedService
    def vcsService
    def repositoryFileService
    String cacheDir

    def setup() {
        jummpIntegrationTest = new JummpIntegrationTest()
        cacheDir = File.createTempDir().getAbsolutePath()
        repositoryFileService.setModelCacheDir(cacheDir)
    }

    def doWithConfig(c) {
        c.jummp.model.cache.dir = cacheDir
    }

    def cleanup() {
        File wd = new File("target/vcs/wd")
        File ed = new File("target/vcs/ed")
        if (wd.exists()) {
            FileUtils.deleteDirectory(wd)
        }
        if (ed.exists()) {
            FileUtils.deleteDirectory(ed)
        }
        Path cachePath = Paths.get(cacheDir)
        cachePath.deleteDir()
    }

    /**
     * The test context: when the given model is attached a publication and this test aims at checking the association
     * between the model and the publication.
     */
    def "test get method of RepositoryFileService"() {
        setup: "accompanying components"
        // working and exchange directory
        File workingDir = new File("target/vcs/wd/ppp/")
        workingDir.mkdirs()
        File exchangeDir = new File("target/vcs/ed/")
        exchangeDir.mkdirs()
        String rootPath = workingDir.getParent()

        when: "initialise services' properties"
        grailsApplication.config.jummp.vcs.workingDirectory = rootPath
        grailsApplication.config.jummp.vcs.exchangeDirectory = exchangeDir.path
        String REGISTRY_EXPORT_FILE_NAME = "testMiriam.xml"
        miriamService.registryExport = new File(exchangeDir.path, REGISTRY_EXPORT_FILE_NAME)
        String currentContainer = workingDir.getCanonicalPath()
        fileSystemService.currentModelContainer.set(currentContainer)
        fileSystemService.root = workingDir.getParentFile()
        vcsService.modelContainerRoot = rootPath
        def gitFactory = grailsApplication.mainContext.getBean("gitManagerFactory")
        vcsService.vcsManager = gitFactory.getInstance()
        vcsService.vcsManager.exchangeDirectory = exchangeDir
        then: "they are successfully initialised"
        workingDir.exists()
        exchangeDir.exists()
        null != rootPath
        null != grailsApplication.config.jummp.vcs.workingDirectory
        vcsService.isValid()

        when: "create a user, authenticate him"
        /**
         * The model files will be uploaded in a dedicated transaction in which the hibernate couldn't see the user
         * owning the model. The user should exist in User table for looking up later. Thus, creating user and roles
         * must be in a dedicated transaction.
         */
        def txDefinition = [propagationBehavior: TransactionDefinition.PROPAGATION_REQUIRES_NEW]
        Model.withTransaction(txDefinition) {
            jummpIntegrationTest.createUserAndRoles()
        }
        jummpIntegrationTest.authenticateAsUser()
        then: "test the newly created user"
        User.findByUsername("username").username == "username"

        when: "create a model"
        File f = new File("test/files/BIOMD0000000272.xml")
        f.exists()
        String name = JummpXmlUtils.findModelAttribute(f, "model", "name").trim()
        null != name

        def rf = new RepositoryFileTransportCommand(path: f.absolutePath, description: "SBML representation", mainFile: true)
        def fmt = new ModelFormatTransportCommand(identifier: "SBML", formatVersion: "L2V4")
        final String pid = "22761472"

        PublicationTransportCommand publication = pubMedService.fetchPublicationData(pid)
        def mtc = new ModelTransportCommand(publication: publication)
        def rev = new RevisionTransportCommand(name: name, validated: true, format: fmt, model: mtc, owner: "username")
        Model m = modelService.uploadValidatedModel([rf], rev)
        Model.withSession { s ->
            s.flush()
            s.clear()
        }
        then: "the model was saved successfully"
        assert publication
        assert m.publication

        when: "load the model and publication"
        Model alterEgo = Model.load(m.id)
        Revision firstCommit = modelService.getLatestRevision(alterEgo, false)
        Publication alterPub = firstCommit.model.publication
        List<File> rfs = vcsService.retrieveFiles(firstCommit)
        RevisionTransportCommand rtc = modelDelegateService.getRevisionFromParams("${m.submissionId}", null)
        PublicationTransportCommand ptc = rtc.model.publication

        String journal = "Clinical cancer research : an official journal of the American Association for Cancer Research"
        String title = "A tumor growth inhibition model for low-grade glioma treated with chemotherapy or radiotherapy."
        String pages = "5071-5080"

        List<File> modelFiles = repositoryFileService.get(firstCommit)
        then: "test the publication is loaded fully"
        null != rtc
        null != ptc
        final ModelException exception = thrown()
        final String message = "The files associated with this model ${firstCommit.model.submissionId}, revision 1 hasn't been cached yet"
        exception.message == message
        when: "trying to retrieve files of the model"
        modelFiles = repositoryFileService.retrieveFiles(firstCommit)
        then: "there is a main file"
        1 == modelFiles.size()
        expect: "the publication was saved and its metadata is matched with the actual values"
        /**
         * Test Model's Files
         */
        1 == rfs.size()
        "BIOMD0000000272.xml" == rfs.first().name

        /**
         * Test Publication
         */
        null != firstCommit
        alterEgo.id == firstCommit.model.id
        assert alterPub
        journal == alterPub.journal
        title == alterPub.title
        alterPub.affiliation.length() > 0
        alterPub.synopsis.length() > 0
        pages == alterPub.pages

        /**
         * Test PublicationTransportCommand
         */
        null != rtc
        null != ptc
        journal == ptc.journal
        title == ptc.title
        pages == ptc.pages
    }
}
