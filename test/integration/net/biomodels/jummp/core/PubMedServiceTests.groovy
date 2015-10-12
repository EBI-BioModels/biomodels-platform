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
 * JUnit (or a modified version of that library), containing parts
 * covered by the terms of Common Public License, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of JUnit used as well as
 * that of the covered work.}
 **/





package net.biomodels.jummp.core

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import net.biomodels.jummp.core.model.*
import net.biomodels.jummp.core.util.JummpXmlUtils
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.RepositoryFile
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.model.PublicationPerson
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User
import org.apache.commons.io.FileUtils
import org.junit.*

import static org.junit.Assert.*

@TestMixin(IntegrationTestMixin)
class PubMedServiceTests extends JummpIntegrationTest {
    def grailsApplication
    def fileSystemService
    def modelService
    def modelDelegateService
    def pubMedService
    def userService
    def vcsService
    def miriamService
    def solrServerHolder

    @Test
    void testFetchPublicationData() {
        authenticateAnonymous()
        Person fakeAuthor = new Person(userRealName: "Not Schilling",
                    orcid: "0000-0002-9517-5166")
        fakeAuthor.save()
        User user = new User(username: "fakeUser",
                     password: "nopassword",
                     person: fakeAuthor,
                     email: "noemail@hotmail.com")
        userService.register(user)
        // publication: Science   (ISSN: 0036-8075)   (ESSN: 1095-9203)
        String id = "20488988"
        Publication publication = pubMedService.fromCommandObject(pubMedService.fetchPublicationData(id))
        assertTrue(publication.validate())
        assertEquals("Science (New York, N.Y.)", publication.journal)
        assertEquals(2010, publication.year)
        assertEquals("6", publication.month)
        //assertEquals(11, publication.day) DONT ALWAYS GET BACK DAY FROM NEW PUBMED SERVICE
        assertEquals(328, publication.volume)
        assertEquals(5984, publication.issue)
        assertEquals("1404-1408", publication.pages)
        assertEquals("Covering a broad dynamic range: information processing at the erythropoietin receptor.", publication.title)
        assertEquals("Division Systems Biology of Signal Transduction, DKFZ-ZMBH Alliance, German Cancer Research Center, 69120 Heidelberg, Germany.", publication.affiliation)
        assertEquals("Cell surface receptors convert extracellular cues into receptor activation, thereby triggering intracellular signaling networks and controlling cellular decisions. A major unresolved issue is the identification of receptor properties that critically determine processing of ligand-encoded information. We show by mathematical modeling of quantitative data and experimental validation that rapid ligand depletion and replenishment of the cell surface receptor are characteristic features of the erythropoietin (Epo) receptor (EpoR). The amount of Epo-EpoR complexes and EpoR activation integrated over time corresponds linearly to ligand input; this process is carried out over a broad range of ligand concentrations. This relation depends solely on EpoR turnover independent of ligand binding, which suggests an essential role of large intracellular receptor pools. These receptor properties enable the system to cope with basal and acute demand in the hematopoietic system.", publication.synopsis)
        // TODO: add tests for author
        def authorTest = PublicationPerson.findByPublicationAndPerson(publication, fakeAuthor)
        assertNotNull(authorTest)
        assertEquals(authorTest.person, fakeAuthor)
        assertEquals(authorTest.person.userRealName, "Not Schilling")
        assertEquals(authorTest.pubAlias, "Schilling M")
        assertEquals(authorTest.position, 1)
        // test for 12974500 - no day specified
        publication = pubMedService.fromCommandObject(pubMedService.fetchPublicationData("12974500"))
        assertNull(publication.day)

        // test for 20955552 - no month and no issue
        publication = pubMedService.fromCommandObject(pubMedService.fetchPublicationData("20955552"))
        assertNull(publication.day)
        assertEquals("0",publication.month)
        assertNull(publication.issue)
    }

    void testModelPublicationAssociation() {
        def container = new File("target/vcs/wd/ppp/")
        String rootPath = container.getParent()
        container.mkdirs()
        assertTrue container.exists()
        String currentContainer = container.getCanonicalPath()
        def exchange = new File("target/vcs/ed/")
        exchange.mkdirs()
        assertTrue exchange.exists()
        grailsApplication.config.jummp.vcs.workingDirectory = rootPath
        grailsApplication.config.jummp.vcs.exchangeDirectory = exchange.path
        String REGISTRY_EXPORT_FILE_NAME = "testMiriam.xml"
        miriamService.registryExport = new File(exchange.path, REGISTRY_EXPORT_FILE_NAME)
        fileSystemService.currentModelContainer = currentContainer
        fileSystemService.root = container.getParentFile()
        vcsService.modelContainerRoot = rootPath
        def gitFactory = grailsApplication.mainContext.getBean("gitManagerFactory")
        vcsService.vcsManager = gitFactory.getInstance()
        vcsService.vcsManager.exchangeDirectory = exchange
        assertTrue(vcsService.isValid())
        assertNotNull solrServerHolder
        createUserAndRoles()
        authenticateAsUser()

        File f = new File("test/files/BIOMD0000000272.xml")
        assertTrue f.exists()
        String name = JummpXmlUtils.findModelAttribute(f, "model", "name").trim()
        assertNotNull name
        def rf = new RepositoryFileTransportCommand(path: f.absolutePath, description: "",
                mainFile: true)
        def fmt = new ModelFormatTransportCommand(identifier: "SBML",
                formatVersion: "L2V4")
        final String pid = "22761472"
        PublicationTransportCommand publication = pubMedService.fetchPublicationData(pid)
        assertNotNull publication
        def mtc = new ModelTransportCommand(publication: publication)
        def rev = new RevisionTransportCommand(name: name, validated: true, format: fmt,
                model: mtc)
        Model m = modelService.uploadValidatedModel([rf], rev)
        assertNotNull m.publication
        Model.withSession { s ->
            s.flush()
            s.clear()
        }

        String journal = "Clinical cancer research : an official journal of the American Association for Cancer Research"
        String title = "A tumor growth inhibition model for low-grade glioma treated with chemotherapy or radiotherapy."
        String pages = "5071-5080"
        Model.withNewSession {
            Model alterEgo = Model.load(m.id)
            Revision firstCommit = modelService.getLatestRevision(alterEgo, false)
            assertNotNull firstCommit
            assertEquals alterEgo.id, firstCommit.model.id
            Publication alterPub = firstCommit.model.publication
            assertNotNull alterPub
            assertEquals journal, alterPub.journal
            assertEquals title, alterPub.title
            assertTrue alterPub.affiliation.length() > 0
            assertTrue alterPub.synopsis.length() > 0
            assertEquals pages, alterPub.pages
            List<RepositoryFile> rfs = vcsService.retrieveFiles(firstCommit)
            assertEquals 1, rfs.size()
            assertEquals "BIOMD0000000272.xml", rfs.first().name
        }

        Model.withNewSession {
            RevisionTransportCommand rtc = modelDelegateService.getRevisionFromParams("${m.submissionId}", null)
            assertNotNull rtc
            PublicationTransportCommand ptc = rtc.model.publication
            assertNotNull ptc
            assertEquals journal, ptc.journal
            assertEquals title, ptc.title
            assertEquals pages, ptc.pages
        }

        solrServerHolder.server.deleteByQuery("*:*")
        solrServerHolder.server.commit()
        FileUtils.deleteDirectory(new File("target/vcs/wd"))
        FileUtils.deleteDirectory(new File("target/vcs/ed"))
    }
}
