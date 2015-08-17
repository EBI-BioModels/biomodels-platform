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
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.core.util.JummpXmlUtils
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.git.GitManagerFactory
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.webapp.ModelController
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.*
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.domain.BasePermission
import net.biomodels.jummp.core.adapters.DomainAdapter

@TestMixin(IntegrationTestMixin)
class ModelServiceTests extends JummpIntegrationTest {
    def aclUtilService
    def modelService
    def modelFileFormatService
    def fileSystemService
    def grailsApplication

    @Before
    void setUp() {
        def container = new File("target/vcs/git/ggg/")
        String rootPath = container.getParent()
        container.mkdirs()
        assertTrue container.exists()
        String currentContainer = container.getCanonicalPath()
        def exchange = new File("target/vcs/exchange/")
        exchange.mkdirs()
        grailsApplication.config.jummp.vcs.workingDirectory = rootPath
        grailsApplication.config.jummp.vcs.exchangeDirectory = exchange.path
        assertTrue exchange.exists()
        fileSystemService.currentModelContainer = currentContainer
        fileSystemService.root = container.getParentFile()
        modelService.vcsService.modelContainerRoot = rootPath
        def gitFactory = grailsApplication.mainContext.getBean("gitManagerFactory")
        modelService.vcsService.vcsManager = gitFactory.getInstance()
        modelService.vcsService.vcsManager.exchangeDirectory = exchange
        assertTrue(modelService.vcsService.isValid())
        createUserAndRoles()
    }

    @After
    void tearDown() {
        try {
            FileUtils.deleteDirectory(new File("target/vcs/git"))
            FileUtils.deleteDirectory(new File("target/vcs/exchange"))
        } catch(Exception ignore) {
        }
        modelService.vcsService.vcsManager = null
        modelService.modelFileFormatService = modelFileFormatService
        modelService.vcsService.modelContainerRoot = null
        fileSystemService.currentModelContainer = null
    }

    @Test
    void testGetAllModelsSecurity() {
        Model model = new Model(vcsIdentifier: "test/", submissionId: "M1")
        FileUtils.touch(new File("${fileSystemService.findCurrentModelContainer()}/${model.vcsIdentifier}test.xml".toString()))
        Revision revision = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision.validate())
        model.addToRevisions(revision)
        assertTrue(model.validate())
        model.save()
        // testUser should not see the model
        authenticateAsTestUser()
        assertTrue(modelService.getAllModels().isEmpty())
        assertEquals(0, modelService.getModelCount())
        // admin should see one model
        authenticateAsAdmin()
        assertFalse(modelService.getAllModels().isEmpty())
        assertSame(model, modelService.getAllModels().first())
        assertEquals(1, modelService.getModelCount())
        // user 2 should not see the model
        authenticateAsUser()
        assertTrue(modelService.getAllModels().isEmpty())
        assertEquals(0, modelService.getModelCount())
        // adding an acl for user 2
        aclUtilService.addPermission(revision, "username", BasePermission.READ)
        // now user 2 should see the model
        assertFalse(modelService.getAllModels().isEmpty())
        assertSame(model, modelService.getAllModels().first())
        assertEquals(1, modelService.getModelCount())
    }

    @Test
    void testModelCount() {
        // no models, should return 0 for all users
        authenticateAsTestUser()
        assertEquals(0, modelService.getModelCount())
        authenticateAsUser()
        assertEquals(0, modelService.getModelCount())
        authenticateAsAdmin()
        assertEquals(0, modelService.getModelCount())
        // create one model
        Model model = new Model(vcsIdentifier: "test/", submissionId: "m1")
        FileUtils.touch(new File("${fileSystemService.findCurrentModelContainer()}/${model.vcsIdentifier}test.xml".toString()))
        Revision revision = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision.validate())
        model.addToRevisions(revision)
        assertTrue(model.validate())
        model.save()
        // no auth is set, only admin should see it
        authenticateAsTestUser()
        assertEquals(0, modelService.getModelCount())
        authenticateAsUser()
        assertEquals(0, modelService.getModelCount())
        authenticateAsAdmin()
        assertEquals(1, modelService.getModelCount())
        // adding read permission for all users should render one model for all
        aclUtilService.addPermission(revision, "testuser", BasePermission.READ)
        aclUtilService.addPermission(revision, "username", BasePermission.READ)
        aclUtilService.addPermission(revision, "admin", BasePermission.READ)
        authenticateAsTestUser()
        assertEquals(1, modelService.getModelCount())
        authenticateAsUser()
        assertEquals(1, modelService.getModelCount())
        authenticateAsAdmin()
        assertEquals(1, modelService.getModelCount())
        // admin user should also see it if he is not modelled as admin
        modelAdminUser(false)
        authenticate("admin", "1234")
        assertEquals(1, modelService.getModelCount())
        // verify adding another revision with no read permission
        Revision revision2 = new Revision(model: model, vcsId: "2", revisionNumber: 2, owner: User.findByUsername("username"), minorRevision: true, name:"", description:"", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision2.validate())
        model.addToRevisions(revision2)
        assertTrue(model.validate())
        model.save()
        authenticateAsTestUser()
        assertEquals(1, modelService.getModelCount())
        authenticateAsUser()
        assertEquals(1, modelService.getModelCount())
        authenticateAsAdmin()
        assertEquals(1, modelService.getModelCount())
        // admin user should also see it if he is not modelled as admin
        modelAdminUser(false)
        authenticate("admin", "1234")
        assertEquals(1, modelService.getModelCount())
        // create another ten models
        for (int i=0; i<10; i++) {
            Model m = new Model(vcsIdentifier: "target/vcs/git/test${i}/", submissionId: "MODEL$i")
            FileUtils.touch(new File("${fileSystemService.findCurrentModelContainer()}/${m.vcsIdentifier}test${i}.xml".toString()))
            Revision r = new Revision(model: m, vcsId: "rev${i}", revisionNumber: 1, owner: User.findByUsername("testuser"), minorRevision: false, name:"${i}", description:"", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
            assertTrue(r.validate())
            m.addToRevisions(r)
            assertTrue(m.validate())
            m.save()
        }
        // admin should see all 11 models
        authenticateAsAdmin()
        assertEquals(11, modelService.getModelCount())
        // there are no permissions set yet - users should only see one model
        authenticateAsTestUser()
        assertEquals(1, modelService.getModelCount())
        authenticateAsUser()
        assertEquals(1, modelService.getModelCount())
        // user 1 gets first three models, user 2 gets next three and both get next four models, last not assigned
        aclUtilService.addPermission(Revision, revision2.id + 1, "testuser", BasePermission.READ)
        aclUtilService.addPermission(Revision, revision2.id + 2, "testuser", BasePermission.READ)
        aclUtilService.addPermission(Revision, revision2.id + 3, "testuser", BasePermission.READ)
        aclUtilService.addPermission(Revision, revision2.id + 4, "username",     BasePermission.READ)
        aclUtilService.addPermission(Revision, revision2.id + 5, "username",     BasePermission.READ)
        aclUtilService.addPermission(Revision, revision2.id + 6, "username",     BasePermission.READ)
        aclUtilService.addPermission(Revision, revision2.id + 7, "ROLE_USER",     BasePermission.READ)
        aclUtilService.addPermission(Revision, revision2.id + 8, "ROLE_USER",     BasePermission.READ)
        aclUtilService.addPermission(Revision, revision2.id + 9, "ROLE_USER",     BasePermission.READ)
        // admin should see all 11 models
        authenticateAsAdmin()
        assertEquals(11, modelService.getModelCount())
        // both users should get seven models
        authenticateAsTestUser()
        assertEquals(7, modelService.getModelCount())
        authenticateAsUser()
        assertEquals(7, modelService.getModelCount())
        // let's delete the very first model
        authenticateAsAdmin()
        assertTrue(modelService.deleteModel(model))
        assertEquals(10, modelService.getModelCount())
        // both users should get six models
        authenticateAsTestUser()
        assertEquals(6, modelService.getModelCount())
        authenticateAsUser()
        assertEquals(6, modelService.getModelCount())
    }

    @Test
    void testGetLatestRevision() {
        // create Model with one revision, without ACL
        Model model = new Model(vcsIdentifier: "test/", submissionId: "m1")
        FileUtils.touch(new File("${fileSystemService.findCurrentModelContainer()}/${model.vcsIdentifier}test.xml".toString()))
        Revision revision = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description:"", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision.validate())
        model.addToRevisions(revision)
        assertTrue(model.validate())
        model.save()
        // only admin should see the revision
        authenticateAsTestUser()
        assertNull(modelService.getLatestRevision(model))
        authenticateAsUser()
        assertNull(modelService.getLatestRevision(model))
        authenticateAsAdmin()
        assertNotNull(modelService.getLatestRevision(model))
        assertSame(revision, modelService.getLatestRevision(model))
        // adding permission for the users
        aclUtilService.addPermission(revision, "username", BasePermission.READ)
        aclUtilService.addPermission(revision, "testuser", BasePermission.READ)
        // now our users should see the revision
        authenticateAsTestUser()
        assertNotNull(modelService.getLatestRevision(model))
        assertSame(revision, modelService.getLatestRevision(model))
        authenticateAsUser()
        assertNotNull(modelService.getLatestRevision(model))
        assertSame(revision, modelService.getLatestRevision(model))
        authenticateAsAdmin()
        assertNotNull(modelService.getLatestRevision(model))
        assertSame(revision, modelService.getLatestRevision(model))
        // add some more revisions
        Revision rev2 = new Revision(model: model, vcsId: "2", revisionNumber: 2, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev3 = new Revision(model: model, vcsId: "3", revisionNumber: 3, owner: User.findByUsername("username"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev4 = new Revision(model: model, vcsId: "4", revisionNumber: 4, owner: User.findByUsername("username"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev5 = new Revision(model: model, vcsId: "5", revisionNumber: 5, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev6 = new Revision(model: model, vcsId: "6", revisionNumber: 6, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(rev2.validate())
        assertTrue(rev3.validate())
        assertTrue(rev4.validate())
        assertTrue(rev5.validate())
        assertTrue(rev6.validate())
        model.addToRevisions(rev2)
        model.addToRevisions(rev3)
        model.addToRevisions(rev4)
        model.addToRevisions(rev5)
        model.addToRevisions(rev6)
        assertTrue(model.validate())
        model.save()
        // no acl set for these new revisions, user should still see previous revision, admin should see rev6
        authenticateAsTestUser()
        assertSame(revision, modelService.getLatestRevision(model))
        authenticateAsUser()
        assertSame(revision, modelService.getLatestRevision(model))
        authenticateAsAdmin()
        assertSame(rev6, modelService.getLatestRevision(model))
        // let's add some ACL
        aclUtilService.addPermission(rev3, "testuser", BasePermission.READ)
        aclUtilService.addPermission(rev6, "username", BasePermission.READ)
        authenticateAsTestUser()
        assertSame(rev3, modelService.getLatestRevision(model))
        authenticateAsUser()
        assertSame(rev6, modelService.getLatestRevision(model))
        // allow rev5 for all users
        aclUtilService.addPermission(rev5, "ROLE_USER", BasePermission.READ)
        authenticateAsTestUser()
        assertSame(rev5, modelService.getLatestRevision(model))
        authenticateAsUser()
        assertSame(rev6, modelService.getLatestRevision(model))
        authenticateAsAdmin()
        assertSame(rev6, modelService.getLatestRevision(model))

        def modelController = new ModelController()
        modelController.params.id = "${model.submissionId}"
        modelController.show()

        // let's delete the model
        authenticateAsAdmin()
        assertTrue(modelService.deleteModel(model))
        assertSame(rev6, modelService.getLatestRevision(model))
        assertFalse(modelService.canPublish(rev6))
        assertFalse(modelService.canAddRevision(model))
    }

    @Test
    void testGetByRevisionIdentifier() {
        // create Model with one revision, without ACL
        Model model = new Model(vcsIdentifier: "test/", submissionId: "m2")
        FileUtils.touch(new File("${fileSystemService.findCurrentModelContainer()}/${model.vcsIdentifier}test.xml".toString()))
        Revision revision = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description:"", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision.validate())
        model.addToRevisions(revision)
        assertTrue(model.validate())
        model.save()
        // only admin should see the revision
        authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
        	modelService.getRevision("${model.submissionId}")
        }
        authenticateAsUser()
        shouldFail(AccessDeniedException) {
        	modelService.getRevision("${model.submissionId}")
        }
        authenticateAsAdmin()
        assertSame(revision, modelService.getRevision("${model.submissionId}"))
        // adding permission for the users
        aclUtilService.addPermission(revision, "username", BasePermission.READ)
        aclUtilService.addPermission(revision, "testuser", BasePermission.READ)
        // now our users should see the revision
        authenticateAsTestUser()
        assertSame(revision, modelService.getRevision("${model.submissionId}"))
        authenticateAsUser()
        assertSame(revision, modelService.getRevision("${model.submissionId}"))
        authenticateAsAdmin()
        assertSame(revision, modelService.getRevision("${model.submissionId}"))
        // add some more revisions
        Revision rev2 = new Revision(model: model, vcsId: "2", revisionNumber: 2, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev3 = new Revision(model: model, vcsId: "3", revisionNumber: 3, owner: User.findByUsername("username"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev4 = new Revision(model: model, vcsId: "4", revisionNumber: 4, owner: User.findByUsername("username"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev5 = new Revision(model: model, vcsId: "5", revisionNumber: 5, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev6 = new Revision(model: model, vcsId: "6", revisionNumber: 6, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(rev2.validate())
        assertTrue(rev3.validate())
        assertTrue(rev4.validate())
        assertTrue(rev5.validate())
        assertTrue(rev6.validate())
        model.addToRevisions(rev2)
        model.addToRevisions(rev3)
        model.addToRevisions(rev4)
        model.addToRevisions(rev5)
        model.addToRevisions(rev6)
        assertTrue(model.validate())
        model.save()
        // no acl set for these new revisions, user should still see previous revision, admin should see rev6
        authenticateAsTestUser()
        assertSame(revision, modelService.getRevision("${model.submissionId}"))
        authenticateAsUser()
        assertSame(revision, modelService.getRevision("${model.submissionId}"))
        authenticateAsAdmin()
        assertSame(rev6, modelService.getRevision("${model.submissionId}"))
        // let's add some ACL
        aclUtilService.addPermission(rev3, "testuser", BasePermission.READ)
        aclUtilService.addPermission(rev6, "username", BasePermission.READ)
        authenticateAsTestUser()
        assertSame(rev3, modelService.getRevision("${model.submissionId}.${rev3.revisionNumber}"))
        authenticateAsUser()
        assertSame(rev6, modelService.getRevision("${model.submissionId}.${rev6.revisionNumber}"))
        // admin should see everything
        authenticateAsAdmin()
        assertSame(rev2, modelService.getRevision("${model.submissionId}.${rev2.revisionNumber}"))
        assertSame(rev3, modelService.getRevision("${model.submissionId}.${rev3.revisionNumber}"))
        assertSame(rev4, modelService.getRevision("${model.submissionId}.${rev4.revisionNumber}"))
        assertSame(rev5, modelService.getRevision("${model.submissionId}.${rev5.revisionNumber}"))
        assertSame(rev6, modelService.getRevision("${model.submissionId}.${rev6.revisionNumber}"))
    }

    @Test
    @SuppressWarnings('UnusedVariable')
    void testGetAllModels() {
        30.times { i ->
            // create thirty models
            final String rootPath = fileSystemService.root.getCanonicalPath()
            // the vcs identifier for a model should always be relative to the root where all models are stored
            def prefix = fileSystemService.findCurrentModelContainer() - rootPath
            Model m = new Model(vcsIdentifier: "${prefix}/test${i}/", submissionId: "MDL$i")
            FileUtils.touch(new File("${rootPath}/${m.vcsIdentifier}/test${i}.xml".toString()).getCanonicalFile())
            Revision r = new Revision(model: m, vcsId: "rev${i}", revisionNumber: 1, owner: User.findByUsername("testuser"), minorRevision: false, name:"${i}", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
            assertTrue(r.validate())
            m.addToRevisions(r)
            assertTrue(m.validate())
            m.save()
        }
        // change to admin
        authenticateAsAdmin()
        // just using getAllModels without parameters should return first ten elements
        List<Model> testElements = modelService.getAllModels()
        assertEquals(10, testElements.size())
        assertSame(Revision.findByName("0").model, testElements.first())
        // use a loop on all elements
        10.times { i ->
            assertSame(Revision.findByName("${i}").model, testElements[i])
        }
        // get five elements starting from 15
        testElements = modelService.getAllModels(15, 5)
        assertEquals(5, testElements.size())
        for (int i=0; i<5; i++) {
            assertSame(Revision.findByName("${15+i}").model, testElements[i])
        }
        // try inverted ordering offset 5 and 6 elements
        testElements = modelService.getAllModels(5, 6, false)
        assertEquals(6, testElements.size())
        for (int i=0; i<6; i++) {
            assertSame(Revision.findByName("${24-i}").model, testElements[i])
        }
        // add some permissions - each second becomes visible to users
        for (int i=0; i<15; i++) {
            aclUtilService.addPermission(Revision.findByVcsId("rev${i*2}"), "ROLE_USER", BasePermission.READ)
        }
        authenticateAsTestUser()
        // get first ten elements
        testElements = modelService.getAllModels()
        assertEquals(10, testElements.size())
        assertSame(Revision.findByName("0").model, testElements.first())
        // use a loop on all elements
        for (int i=0; i<10; i++) {
            assertSame(Revision.findByName("${i*2}").model, testElements[i])
        }
        // get five elements starting from 5
        testElements = modelService.getAllModels(5, 5)
        assertEquals(5, testElements.size())
        for (int i=0; i<5; i++) {
            assertSame(Revision.findByName("${10+i*2}").model, testElements[i])
        }
        // try inverted ordering offset 5 and 6 elements
        testElements = modelService.getAllModels(5, 6, false)
        assertEquals(6, testElements.size())
        for (int i=0; i<6; i++) {
            assertSame(Revision.findByName("${18-i*2}").model, testElements[i])
        }
        // some border case tests
        // 0 elements
        assertEquals(0, modelService.getAllModels(0, 0).size())
        // index out of bounds
        assertEquals(0, modelService.getAllModels(30, 10).size())
        // negative offset and count
        assertEquals(0, modelService.getAllModels(-2, -2).size())
        // with offset and count expanding boundaries
        testElements = modelService.getAllModels(13, 5)
        assertEquals(2, testElements.size())
        assertSame(Revision.findByName("${13*2}").model, testElements.first())
        assertSame(Revision.findByName("${14*2}").model, testElements.last())
        // same as admin user
        authenticateAsAdmin()
        testElements = modelService.getAllModels(28, 5)
        assertEquals(2, testElements.size())
        assertSame(Revision.findByName("${28}").model, testElements.first())
        assertSame(Revision.findByName("${29}").model, testElements.last())
        // delete the very first model
        modelService.deleteModel(Revision.findByName("0").model)
        testElements = modelService.getAllModels()
        assertEquals(10, testElements.size())
        assertSame(Revision.findByName("1").model, testElements.first())
        authenticateAsTestUser()
        testElements = modelService.getAllModels()
        assertEquals(10, testElements.size())
        assertSame(Revision.findByName("2").model, testElements.first())
    }

    @Test
    @SuppressWarnings('UnusedVariable')
    void testGetAllRevisions() {
        // create one model with one revision and no acl
         final String rootPath = fileSystemService.root.getCanonicalPath()
        // the vcs identifier for a model should always be relative to the root where all models are stored
        def prefix = fileSystemService.findCurrentModelContainer() - rootPath
        Model model = new Model(vcsIdentifier: "${prefix}/test/".toString(), submissionId: "M1")
//        FileUtils.touch(new File("${rootPath}/${m.vcsIdentifier}/test.xml".toString()).getCanonicalFile())
        Revision revision = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("testuser"), minorRevision: false, name: "test", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision.validate())
        model.addToRevisions(revision)
        assertTrue(model.validate())
        model.save()
        // only admin should see the revision
        authenticateAsTestUser()
        assertEquals(0, modelService.getAllRevisions(model).size())
        authenticateAsUser()
        assertEquals(0, modelService.getAllRevisions(model).size())
        authenticateAsAdmin()
        assertEquals(1, modelService.getAllRevisions(model).size())
        assertSame(revision, modelService.getAllRevisions(model).first())
        // adding permission should user make see it
        aclUtilService.addPermission(revision, "ROLE_USER", BasePermission.READ)
        authenticateAsTestUser()
        assertEquals(1, modelService.getAllRevisions(model).size())
        assertSame(revision, modelService.getAllRevisions(model).first())
        authenticateAsUser()
        assertEquals(1, modelService.getAllRevisions(model).size())
        assertSame(revision, modelService.getAllRevisions(model).first())
        authenticateAsAdmin()
        assertEquals(1, modelService.getAllRevisions(model).size())
        assertSame(revision, modelService.getAllRevisions(model).first())
        // add some more revisions without ACL
        Revision rev1 = new Revision(model: model, vcsId: "2", revisionNumber: 2, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev2 = new Revision(model: model, vcsId: "3", revisionNumber: 3, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev3 = new Revision(model: model, vcsId: "4", revisionNumber: 4, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev4 = new Revision(model: model, vcsId: "5", revisionNumber: 5, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev5 = new Revision(model: model, vcsId: "6", revisionNumber: 6, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(rev1.validate())
        assertTrue(rev2.validate())
        assertTrue(rev3.validate())
        assertTrue(rev4.validate())
        assertTrue(rev5.validate())
        model.addToRevisions(rev1)
        model.addToRevisions(rev2)
        model.addToRevisions(rev3)
        model.addToRevisions(rev4)
        model.addToRevisions(rev5)
        assertTrue(model.validate())
        model.save()
        // admin should see all revisions, users the previous ones
        authenticateAsTestUser()
        assertEquals(1, modelService.getAllRevisions(model).size())
        assertSame(revision, modelService.getAllRevisions(model).first())
        authenticateAsUser()
        assertEquals(1, modelService.getAllRevisions(model).size())
        assertSame(revision, modelService.getAllRevisions(model).first())
        authenticateAsAdmin()
        List<Revision> testResults = modelService.getAllRevisions(model)
        assertEquals(6, testResults.size())
        for (int i=0; i<6; i++) {
            assertSame(Revision.findByRevisionNumberAndModel(i+1, model), testResults[i])
        }
        // add some permissions to the revisions
        aclUtilService.addPermission(rev1, "testuser", BasePermission.READ)
        aclUtilService.addPermission(rev3, "testuser", BasePermission.READ)
        aclUtilService.addPermission(rev1, "username", BasePermission.READ)
        aclUtilService.addPermission(rev2, "username", BasePermission.READ)
        aclUtilService.addPermission(rev4, "username", BasePermission.READ)
        aclUtilService.addPermission(rev5, "ROLE_USER", BasePermission.READ)
        // verify that users see the revision
        authenticateAsTestUser()
        testResults = modelService.getAllRevisions(model)
        assertEquals(4, testResults.size())
        assertSame(revision, testResults[0])
        assertSame(rev1, testResults[1])
        assertSame(rev3, testResults[2])
        assertSame(rev5, testResults[3])
        authenticateAsUser()
        testResults = modelService.getAllRevisions(model)
        assertEquals(5, testResults.size())
        assertSame(revision, testResults[0])
        assertSame(rev1, testResults[1])
        assertSame(rev2, testResults[2])
        assertSame(rev4, testResults[3])
        assertSame(rev5, testResults[4])
        // unchanged for admin
        authenticateAsAdmin()
        testResults = modelService.getAllRevisions(model)
        assertEquals(6, testResults.size())
        for (int i=0; i<6; i++) {
            assertSame(Revision.findByRevisionNumberAndModel(i+1, model), testResults[i])
        }
        // add another model
        Model model2 = new Model(vcsIdentifier: "test12.xml", submissionId: "M12")
        Revision revision2 = new Revision(model: model2, vcsId: "12", revisionNumber: 1, owner: User.findByUsername("username"), minorRevision: false, name: "test12", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision2.validate())
        model2.addToRevisions(revision2)
        assertTrue(model2.validate())
        model2.save()
        aclUtilService.addPermission(revision2, "ROLE_USER", BasePermission.READ)
        // nothing should have changed...
        authenticateAsTestUser()
        testResults = modelService.getAllRevisions(model)
        assertEquals(4, testResults.size())
        assertSame(revision, testResults[0])
        assertSame(rev1, testResults[1])
        assertSame(rev3, testResults[2])
        assertSame(rev5, testResults[3])
        authenticateAsUser()
        testResults = modelService.getAllRevisions(model)
        assertEquals(5, testResults.size())
        assertSame(revision, testResults[0])
        assertSame(rev1, testResults[1])
        assertSame(rev2, testResults[2])
        assertSame(rev4, testResults[3])
        assertSame(rev5, testResults[4])
        // unchanged for admin
        authenticateAsAdmin()
        testResults = modelService.getAllRevisions(model)
        assertEquals(6, testResults.size())
        for (int i=0; i<6; i++) {
            assertSame(Revision.findByRevisionNumberAndModel(i+1, model), testResults[i])
        }
        // let's see if we all get the revision for model2
        authenticateAsTestUser()
        testResults = modelService.getAllRevisions(model2)
        assertEquals(1, testResults.size())
        assertSame(revision2, testResults[0])
        authenticateAsUser()
        testResults = modelService.getAllRevisions(model2)
        assertEquals(1, testResults.size())
        assertSame(revision2, testResults[0])
        authenticateAsAdmin()
        testResults = modelService.getAllRevisions(model2)
        assertEquals(1, testResults.size())
        assertSame(revision2, testResults[0])
    }

    @Test
    void testAddRevision() {
        final String rootPath = fileSystemService.root.getCanonicalPath()
        // the vcs identifier for a model should always be relative to the root where all models are stored
        def prefix = fileSystemService.findCurrentModelContainer() - rootPath
        String modelIdentifier = "${prefix}/test/".toString()
        // create one model with one revision and no acl
        Model model = new Model(vcsIdentifier: modelIdentifier, submissionId: "model1")
        Revision revision = new Revision(model: model, vcsId:"1", revisionNumber: 1, owner: User.findByUsername("testuser"), minorRevision: false, name:"test", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision.validate())
        model.addToRevisions(revision)
        assertTrue(model.validate())
        model.save()
        // authenticate as testuser, as there is no ACL he is not allowed to add a revision
        def auth = authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
            File modelFile = new File("${rootPath}/${modelIdentifier}/testMe".toString()).getCanonicalFile()
            FileUtils.touch(modelFile)
            def rf = new RepositoryFileTransportCommand(path: modelFile.absolutePath, description: "", mainFile: true)
            modelService.addRevisionAsFile(model, rf, ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), null)
        }
        // give user the right to write to the model
        authenticateAsAdmin()
        modelService.grantReadAccess(model, User.findByUsername("testuser"))
        modelService.grantWriteAccess(model, User.findByUsername("testuser"))
        // model may not be null - test as admin as otherwise will throw AccessDeniedException
        //modelService.addRevisionAsFile(null, new File("target/test"), ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), "")

        File fileForNullModel = new File("${rootPath}/${modelIdentifier}/test_null".toString()).getCanonicalFile()
        FileUtils.touch(fileForNullModel)
        def rf = new RepositoryFileTransportCommand(path: fileForNullModel.absolutePath, description: "", mainFile: true)
        shouldFail(ModelException) {
            modelService.addRevisionAsList(null, [rf], ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), "")
        }
        // vcs is not yet setup, adding a revision should fail
        authenticateAsTestUser()
        // model may not be null - as a user this throws an AccessDeniedException
        shouldFail(AccessDeniedException) {
            modelService.addRevisionAsFile(null, rf, ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), "")
        }
        // file list may not be null
        shouldFail(ModelException) {
            modelService.addRevisionAsList(model, null, ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), "")
        }

        // file may not be null
        shouldFail(ModelException) {
            modelService.addRevisionAsFile(model, null, ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), "")
        }

        // comment may not be null
        shouldFail(ModelException) {
            modelService.addRevisionAsFile(model, rf, ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), null)
        }
        // file must exist
        shouldFail(ModelException) {
            modelService.addRevisionAsFile(model, new RepositoryFileTransportCommand(
                        path: "target/test/nonexistent.xml", description: "", mainFile: true),
                    ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), "")
        }
        // file may not be a directory
        File exchangeDirectory = new File("target/vcs/exchange")
        exchangeDirectory.mkdirs()
        shouldFail(ModelException) {
            modelService.addRevisionAsFile(model,
                new RepositoryFileTransportCommand(path: exchangeDirectory.path, description: "", mainFile: true),
                    ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), "")
        }
        File importFile = new File("target/vcs/exchange/test.xml")
        FileUtils.touch(importFile)

        /*shouldFail(ModelException) {
            modelService.addRevisionAsFile(model,
                new RepositoryFileTransportCommand(path: importFile.path, description: "", mainFile: true),
                    ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), "")
        }*/

        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        grailsApplication.config.jummp.vcs.exchangeDirectory = "target/vcs/exchange"
        grailsApplication.config.jummp.vcs.workingDirectory = "target/vcs/git"
        grailsApplication.config.jummp.plugins.sbml.validation = true
        modelService.vcsService.vcsManager = gitService.getInstance()
        String currentContainer = fileSystemService.findCurrentModelContainer()
        modelService.vcsService.modelContainerRoot = "target/vcs/git"
        assertTrue(modelService.vcsService.isValid())

        File updateFile = new File("target/vcs/exchange/update.xml")
        updateFile.append("Test\n")
        FileUtils.touch(updateFile)
        rf = new RepositoryFileTransportCommand(path: updateFile.path, description: "", mainFile: true)
        Revision rev = modelService.addRevisionAsFile(model, rf,
                ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), "")
        assertEquals(2, rev.revisionNumber)
        assertTrue(aclUtilService.hasPermission(auth, rev, BasePermission.ADMINISTRATION))
        assertTrue(aclUtilService.hasPermission(auth, rev, BasePermission.READ))
        assertTrue(aclUtilService.hasPermission(auth, rev, BasePermission.DELETE))

        File gitFile = new File("${rootPath}/${modelIdentifier}/update.xml")
        List<String> lines = gitFile.readLines()
        assertEquals(1, lines.size())
        assertEquals("Test", lines[0])
        // user should not have received a read right on the revision
        def auth2 = authenticateAsUser()
        assertFalse(aclUtilService.hasPermission(auth2, rev, BasePermission.READ))
        // grant read access to the user - he should then get right to read the next revision
        authenticateAsAdmin()
        modelService.grantReadAccess(model, User.findByUsername("username"))
        assertTrue(aclUtilService.hasPermission(auth2, model, BasePermission.READ))
        authenticateAsTestUser()
        updateFile.append("Further Test\n")

        Revision rev2 = modelService.addRevisionAsFile(model, rf, ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), "")
        assertEquals(3, rev2.revisionNumber)
        assertFalse(rev2.vcsId == rev.vcsId)
        assertTrue(aclUtilService.hasPermission(auth, rev2, BasePermission.ADMINISTRATION))
        assertTrue(aclUtilService.hasPermission(auth, rev2, BasePermission.READ))
        assertTrue(aclUtilService.hasPermission(auth, rev2, BasePermission.DELETE))
        assertTrue(aclUtilService.hasPermission(auth2, rev2, BasePermission.READ))
        lines = gitFile.readLines()
        assertEquals(2, lines.size())
        assertEquals("Test", lines[0])
        assertEquals("Further Test", lines[1])
        // admin should also be able to import updates
        def adminAuth = authenticateAsAdmin()
        updateFile.append("Admin Test\n")
        Revision rev3 = modelService.addRevisionAsFile(model, rf, ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), "")
        assertEquals(4, rev3.revisionNumber)
        assertFalse(rev3.vcsId == rev2.vcsId)
        assertTrue(aclUtilService.hasPermission(adminAuth, rev3, BasePermission.ADMINISTRATION))
        assertTrue(aclUtilService.hasPermission(adminAuth, rev3, BasePermission.READ))
        assertTrue(aclUtilService.hasPermission(adminAuth, rev3, BasePermission.DELETE))
        assertTrue(aclUtilService.hasPermission(auth, rev3, BasePermission.READ))
        assertTrue(aclUtilService.hasPermission(auth2, rev3, BasePermission.READ))
        lines = gitFile.readLines()
        assertEquals(3, lines.size())
        assertEquals("Test", lines[0])
        assertEquals("Further Test", lines[1])
        assertEquals("Admin Test", lines[2])
        // try adding a revision with invalid sbml file - should not be possible
        File sbmlFile = new File("target/sbml/addRevisionSbmlFile")
        FileUtils.deleteQuietly(sbmlFile)
        FileUtils.touch(sbmlFile)
        rf = new RepositoryFileTransportCommand(path: sbmlFile.path, description: "", mainFile: true)
        shouldFail(ModelException) {
            modelService.addRevisionAsFile(model, rf, ModelFormat.findByIdentifierAndFormatVersion("SBML", "*"), "")
        }
        // with a valid sbml file it should be possible
        sbmlFile = new File("target/sbml/addRevisionValidSbmlFile")
        FileUtils.deleteQuietly(sbmlFile)
        FileUtils.touch(sbmlFile)
        sbmlFile.append('''<?xml version="1.0" encoding="UTF-8"?>
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
</sbml>''')
        rf = new RepositoryFileTransportCommand(path: sbmlFile.path, description: "", mainFile: true)
        Revision rev4 = modelService.addRevisionAsFile(model, rf, ModelFormat.findByIdentifierAndFormatVersion("SBML", "L1V1"), "")
        assertEquals(5, rev4.revisionNumber)
        assertEquals("SBML", rev4.format.identifier)
        // delete the Model - any further updates should end in a ModelException
        modelService.deleteModel(model)
        shouldFail(ModelException) {
            modelService.addRevisionAsFile(model, rf, ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), "")
        }

    }
    @Test
    void testDeleteRestoreModel() {
        Model model = new Model(vcsIdentifier: "test.xml", submissionId: "model1234")
        Revision rev1 = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("testuser"), minorRevision: false, name:"test", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(rev1.validate())
        model.addToRevisions(rev1)
        assertTrue(model.validate())
        model.save()
        // Model is not yet deleted
        assertEquals(ModelState.UNPUBLISHED, rev1.state)
        // try to delete as anonymous
        authenticateAnonymous()
        shouldFail(AccessDeniedException) {
            modelService.deleteModel(model)
        }
        authenticateAsUser()
        shouldFail(AccessDeniedException) {
            modelService.deleteModel(model)
        }
        authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
            modelService.deleteModel(model)
        }
        // admin is allowed to delete
        authenticateAsAdmin()
        assertTrue(modelService.deleteModel(model))
        assertEquals(true, model.deleted)
        // no user is allowed to restore
        authenticateAnonymous()
        shouldFail(AccessDeniedException) {
            modelService.restoreModel(model)
        }
        authenticateAsUser()
        shouldFail(AccessDeniedException) {
            modelService.restoreModel(model)
        }
        authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
            modelService.restoreModel(model)
        }
        // admin can restore the model
        authenticateAsAdmin()
        assertTrue(modelService.restoreModel(model))
        assertEquals(false, model.deleted)
        // let's try to restore again
        assertFalse(modelService.restoreModel(model))
        assertEquals(false, model.deleted)
        // let's grant some rights on the model
        aclUtilService.addPermission(model, "testuser", BasePermission.READ)
        aclUtilService.addPermission(model, "testuser", BasePermission.WRITE)
        aclUtilService.addPermission(model, "testuser", BasePermission.DELETE)
        aclUtilService.addPermission(model, "testuser", BasePermission.ADMINISTRATION)
        // user get's all rights except delete
        aclUtilService.addPermission(model, "username", BasePermission.READ)
        aclUtilService.addPermission(model, "username", BasePermission.WRITE)
        aclUtilService.addPermission(model, "username", BasePermission.ADMINISTRATION)
        // user is still not allowed to delete
        authenticateAsUser()
        shouldFail(AccessDeniedException) {
            modelService.deleteModel(model)
        }
        // testuser is allowed to delete
        authenticateAsTestUser()
        assertTrue(modelService.deleteModel(model))
        assertEquals(true, model.deleted)
        // further delete should not work
        shouldFail(AccessDeniedException) {
        	modelService.deleteModel(model)
        }
        assertEquals(true, model.deleted)
        // testuser is not allowed to restore
        shouldFail(AccessDeniedException) {
            modelService.restoreModel(model)
        }
        // also user is not allowed to restore
        authenticateAsUser()
        shouldFail(AccessDeniedException) {
            modelService.restoreModel(model)
        }
        // admin is allowed to restore
        authenticateAsAdmin()
        assertTrue(modelService.restoreModel(model))
        assertEquals(false, model.deleted)
        // let's change the model state
        Revision savedRev=modelService.getLatestRevision(model)
        savedRev.state = ModelState.UNDER_CURATION
        savedRev.save(flush:true)
        shouldFail(AccessDeniedException) {
        	modelService.deleteModel(model)
        }
        assertEquals(false, model.deleted)
        assertFalse(modelService.restoreModel(model))
        assertEquals(false, model.deleted)
        savedRev.state = ModelState.PUBLISHED
        savedRev.save(flush:true)
        shouldFail(AccessDeniedException) {
        	modelService.deleteModel(model)
        }
        assertEquals(false, model.deleted)
        assertFalse(modelService.restoreModel(model))
        assertEquals(false, model.deleted)
        savedRev.state = ModelState.RELEASED
        savedRev.save(flush:true)
        shouldFail(AccessDeniedException) {
        	modelService.deleteModel(model)
        }
        assertEquals(false, model.deleted)
        assertFalse(modelService.restoreModel(model))
        assertEquals(false, model.deleted)
    }

    @Test
    void testUploadModel() {
        // anonymous user is not allowed to invoke method
        authenticateAnonymous()
        shouldFail(AccessDeniedException) {
            modelService.uploadModelAsFile(null, null)
        }
        // try importing with null file - should fail
        def auth = authenticateAsTestUser()
        ModelTransportCommand meta = new ModelTransportCommand(comment: "Test Comment", format: new ModelFormatTransportCommand(identifier: "UNKNOWN"))

        shouldFail(ModelException) {
            modelService.uploadModelAsFile(null, meta)
        }
        File importFile = new File("target/vcs/exchange/import.xml")
        def rf = new RepositoryFileTransportCommand(path: importFile.absolutePath, description: "", mainFile: true)
        // file does not yet exists = it should fail
        shouldFail(ModelException) {
            modelService.uploadModelAsFile(rf, meta)
        }
        FileUtils.touch(importFile)
        importFile.append("Test\n")
        // also for directory it should fail
        shouldFail(ModelException) {
            rf.path = "target/vcs/exchange/"
            modelService.uploadModelAsFile(rf, meta)
        }
        // VCS system should not be valid - so it should fail
        shouldFail(ModelException) {
            modelService.uploadModelAsFile(rf, meta)
        }
        // now let's create the VCS
        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        grailsApplication.config.jummp.vcs.exchangeDirectory = "target/vcs/exchange"
        grailsApplication.config.jummp.vcs.workingDirectory = "target/vcs/git"
        grailsApplication.config.jummp.plugins.sbml.validation = true
        modelService.vcsService.vcsManager = gitService.getInstance()
        String currentContainer = fileSystemService.findCurrentModelContainer()
        modelService.vcsService.modelContainerRoot = "target/vcs/git"
        assertTrue(modelService.vcsService.isValid())
        rf.path = importFile.absolutePath
        // import should work now
        Model model = modelService.uploadModelAsFile(rf, meta)
        assertTrue(model.validate())
        assertEquals(ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"),
                model.revisions.toList().first().format)
        // complete name cannot be tested, as it uses a generated date and we do not know the date
        assertTrue(model.vcsIdentifier.endsWith("${model.submissionId}/"))
        File parent = new File(grailsApplication.config.jummp.vcs.workingDirectory, model.vcsIdentifier)
        
        File gitFile = new File(parent, importFile.getName())
        List<String> lines = gitFile.readLines()
        assertEquals(1, lines.size())
        assertEquals("Test", lines[0])
        // ensure the revision and commit message is correct
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        Repository repository = builder.setWorkTree(parent)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir(parent) // scan up the file system tree
                .build()
        ObjectId commit = repository.resolve(Constants.HEAD)
        RevWalk revWalk = new RevWalk(repository)
        RevCommit revCommit = revWalk.parseCommit(commit)
        assertEquals(commit.getName(), model.revisions.toList().first().vcsId)
        assertEquals(1, model.revisions.size())
        // we did not specify a commit message, so default should be used
        assertTrue(revCommit.getShortMessage().contains("Imported model at"))
        assertTrue(revCommit.getFullMessage().contains("Imported model at"))
        // verify set permissions
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.READ))
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.ADMINISTRATION))
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.WRITE))
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.DELETE))
        Revision revision = model.revisions.toList().first()
        assertTrue(aclUtilService.hasPermission(auth, revision, BasePermission.READ))
        assertTrue(aclUtilService.hasPermission(auth, revision, BasePermission.ADMINISTRATION))
        assertTrue(aclUtilService.hasPermission(auth, revision, BasePermission.DELETE))
        assertFalse(aclUtilService.hasPermission(auth, revision, BasePermission.WRITE))
        // importing a model with same name should be possible
        File importFile2 = new File("target/vcs/exchange/import2.xml")
        importFile2.setText("I shouldnt need to do this.")
        def rf2 = new RepositoryFileTransportCommand(path: importFile2.absolutePath, description: "", mainFile: true)
        assertTrue((modelService.uploadModelAsFile(rf2, meta)).validate())
        // an invalid submission should yield a model with validated flag set to false
        meta.name = "test2"
        meta.format = DomainAdapter.getAdapter(ModelFormat.findByIdentifierAndFormatVersion("SBML", "*")).toCommandObject()
        File sbmlFile = new File("target/sbml/sbmlTestFile")
        FileUtils.deleteQuietly(sbmlFile)
        FileUtils.touch(sbmlFile)
        sbmlFile.append("this is not an sbml file.")
        rf.path = sbmlFile.absolutePath
        Model theModel = modelService.uploadModelAsFile(rf, meta)
        assertNotNull(theModel)
        Revision theRevision = modelService.getLatestRevision(theModel)
        assertNotNull(theRevision)
        assertFalse(theRevision.validated)
        // importing with valid model file should be possible
        String sbmlText= '''<?xml version="1.0" encoding="UTF-8"?>
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
        sbmlFile = new File("target/sbml/uploadModelValidSbmlFile")
        FileUtils.deleteQuietly(sbmlFile)
        FileUtils.touch(sbmlFile)
        sbmlFile.append(sbmlText)
        rf.path = sbmlFile.absolutePath
        model = modelService.uploadModelAsFile(rf, meta)
        assertTrue(model.validate())
        assertEquals(ModelFormat.findByIdentifierAndFormatVersion("SBML", "*"), model.revisions.toList().first().format)
        assertNotNull(model.revisions.toList().first().uploadDate)
        // test strange characters in the name, which should not end in the file name
        sbmlFile = new File("target/sbml/uploadModelValidSbmlFile2")
        FileUtils.deleteQuietly(sbmlFile)
        FileUtils.touch(sbmlFile)
        sbmlFile.append(sbmlText)
        rf.path = sbmlFile.absolutePath
        meta.name = "test/:/test"
        model = modelService.uploadModelAsFile(rf, meta)
        File gitDirectory = new File(model.vcsIdentifier)
        gitFile = new File(model.vcsIdentifier + System.getProperty("file.separator") + sbmlFile.getName())
        assertTrue(model.validate())
        assertEquals(gitDirectory.getPath(), gitFile.getParent())
        // TODO: somehow we need to test the failing cases, which is non-trivial
        // the only solution were to modify comment to make the revision non-validate, but in future it will be a command object which validates
    }

    @Test
    void testRetrieveModelFiles() {
        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        grailsApplication.config.jummp.vcs.exchangeDirectory = "target/vcs/exchange"
        grailsApplication.config.jummp.vcs.workingDirectory = "target/vcs/git"
        grailsApplication.config.jummp.plugins.sbml.validation = true
        modelService.vcsService.vcsManager = gitService.getInstance()
        String currentContainer = fileSystemService.findCurrentModelContainer()
        modelService.vcsService.modelContainerRoot = "target/vcs/git"
        assertTrue(modelService.vcsService.isValid())
        // import a file
        authenticateAsTestUser()
        ModelTransportCommand meta = new ModelTransportCommand(comment: "Test Comment",
                name: "test", format: new ModelFormatTransportCommand(identifier: "UNKNOWN",
                formatVersion: "*"))
        File importFile = new File("target/vcs/exchange/import.xml")
        FileUtils.touch(importFile)
        importFile.append("Test\n")
        def rf = new RepositoryFileTransportCommand(path: importFile.absolutePath,
                description: "", mainFile: true)
        Model model = modelService.uploadModelAsFile(rf, meta)
        Revision revision = modelService.getLatestRevision(model)
        // Anonymous user should not be allowed to download the revision
        authenticateAnonymous()
        shouldFail(AccessDeniedException) {
            modelService.retrieveModelFiles(revision)
        }
        // User should not be allowed to download the revision
        authenticateAsUser()
        shouldFail(AccessDeniedException) {
            modelService.retrieveModelFiles(revision)
        }
        // as admin we should get a byte array
        authenticateAsAdmin()
        List<RepositoryFileTransportCommand> importFileFromVcs = modelService.retrieveModelFiles(revision)
        assertEquals(1, importFileFromVcs.size())
        def bytes = (new File(importFileFromVcs.first().path)).getBytes()
        assertEquals("Test\n", new String(bytes))
        // as testuser we should also get the byte array
        authenticateAsTestUser()
        List<RepositoryFileTransportCommand> revisionFiles = modelService.retrieveModelFiles(revision)
        assertEquals(1, revisionFiles.size())
        bytes = (new File(revisionFiles.first().path)).getBytes()
        assertEquals("Test\n", new String(bytes))
        // create a random revision
        Revision rev = new Revision(model: model, vcsId: "2", revisionNumber: 2, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        model.addToRevisions(rev)
        model.save(flush: true)
        aclUtilService.addPermission(rev, "testuser", BasePermission.READ)
        // now add another "real" revision
        importFile = new File("target/vcs/exchange/import.xml")
        FileUtils.touch(importFile)
        importFile.setText("Test\nline2\n")
        def repoFile = new RepositoryFileTransportCommand(path: importFile.absolutePath, description: "", mainFile: true)
        Revision rev4 = modelService.addRevisionAsFile(model, repoFile, ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"), "")
        // retrieving the random revision should fail
        shouldFail(ModelException) {
            modelService.retrieveModelFiles(rev)
        }
        // retrieving the proper uploaded revision should 
        List<RepositoryFileTransportCommand> files = modelService.retrieveModelFiles(rev4)
        assertEquals(1, files.size())
        bytes = (new File(files.first().path)).getBytes()
        assertEquals("Test\nline2\n", new String(bytes))
    }

    @Test
    void testGrantReadAccess() {
        // create a model with some revisions
        Model model = new Model(vcsIdentifier: "test.xml", submissionId: "model12345")
        Revision rev1 = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev2 = new Revision(model: model, vcsId: "2", revisionNumber: 2, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev3 = new Revision(model: model, vcsId: "3", revisionNumber: 3, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev4 = new Revision(model: model, vcsId: "4", revisionNumber: 4, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        Revision rev5 = new Revision(model: model, vcsId: "5", revisionNumber: 5, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(rev1.validate())
        assertTrue(rev2.validate())
        assertTrue(rev3.validate())
        assertTrue(rev4.validate())
        assertTrue(rev5.validate())
        model.addToRevisions(rev1)
        model.addToRevisions(rev2)
        model.addToRevisions(rev3)
        model.addToRevisions(rev4)
        model.addToRevisions(rev5)
        assertTrue(model.validate())
        model.save()
        // verify that user cannot access the revisions
        authenticateAsTestUser()
        assertEquals(0, modelService.getAllRevisions(model).size())

        // grant read access to the model as admin
        authenticateAsAdmin()
        modelService.grantReadAccess(model, User.findByUsername("testuser"))
        // user should now see all revisions
        authenticateAsTestUser()
        List<Revision> testResults = modelService.getAllRevisions(model)
        assertEquals(5, testResults.size())
        assertSame(rev1, testResults[0])
        assertSame(rev2, testResults[1])
        assertSame(rev3, testResults[2])
        assertSame(rev4, testResults[3])
        assertSame(rev5, testResults[4])
        // adding one new Revision to the model
        Revision rev6 = new Revision(model: model, vcsId: "6", revisionNumber: 6, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(rev6.validate())
        model.addToRevisions(rev6)
        assertTrue(model.validate())
        assertNotNull(model.save(flush: true))
        // verify that testuser does not have access to this revision
        testResults = modelService.getAllRevisions(Model.get(model.id))
        assertEquals(5, testResults.size())
        assertSame(rev1, testResults[0])
        assertSame(rev2, testResults[1])
        assertSame(rev3, testResults[2])
        assertSame(rev4, testResults[3])
        assertSame(rev5, testResults[4])
        // verify that user is not seeing the revisions
        authenticateAsUser()
        assertEquals(0, modelService.getAllRevisions(model).size())
        // try granting read permission to user - should not change anything
        authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
            modelService.grantReadAccess(model, User.findByUsername("username"))
        }
        authenticateAsUser()
        assertEquals(0, modelService.getAllRevisions(model).size())
        // give admin right to the testuser - this should allow user to grant read access
        authenticateAsAdmin()
        aclUtilService.addPermission(model, "testuser", BasePermission.READ)
        aclUtilService.addPermission(model, "testuser", BasePermission.ADMINISTRATION)
        // if the administration right is not set, the framework throws an exception
        aclUtilService.addPermission(rev1, "testuser", BasePermission.ADMINISTRATION)
        aclUtilService.addPermission(rev2, "testuser", BasePermission.ADMINISTRATION)
        aclUtilService.addPermission(rev3, "testuser", BasePermission.ADMINISTRATION)
        aclUtilService.addPermission(rev4, "testuser", BasePermission.ADMINISTRATION)
        aclUtilService.addPermission(rev5, "testuser", BasePermission.ADMINISTRATION)
        // grant read permission to user
        authenticateAsTestUser()
        modelService.grantReadAccess(Model.get(model.id), User.findByUsername("username"))
        // user should see same revisions as testuser
        authenticateAsUser()
        testResults = modelService.getAllRevisions(model)
        assertEquals(5, testResults.size())
        assertSame(rev1, testResults[0])
        assertSame(rev2, testResults[1])
        assertSame(rev3, testResults[2])
        assertSame(rev4, testResults[3])
        assertSame(rev5, testResults[4])

    }

    @Test
    void testGrantWriteAccess() {
        Model model = new Model(vcsIdentifier: "test.xml", submissionId: "M12345")
        Revision rev1 = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(rev1.validate())
        model.addToRevisions(rev1)
        assertTrue(model.validate())
        model.save()
        // testuser does not have Write permission on model
        def auth = authenticateAsTestUser()
        assertFalse(aclUtilService.hasPermission(auth, model, BasePermission.WRITE))
        authenticateAsAdmin()
        // grant write access
        modelService.grantWriteAccess(model, User.findByUsername("testuser"))
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.WRITE))
        // testuser is not admin to the model, he should not be allowed to grant write permission
        authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
            modelService.grantWriteAccess(model, User.findByUsername("username"))
        }
        // grant admin right to testuser
        authenticateAsAdmin()
        aclUtilService.addPermission(model, "testuser", BasePermission.ADMINISTRATION)
        // verify that user hoes not have the right to write on model
        auth = authenticateAsUser()
        assertFalse(aclUtilService.hasPermission(auth, model, BasePermission.WRITE))
        // grant write right to user
        authenticateAsTestUser()
        modelService.grantWriteAccess(model, User.findByUsername("username"))
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.WRITE))
        // TODO: add checks to verify that uploading a new model revision is allowed
    }

    @Test
    void testRevokeReadAccess() {
        Model model = new Model(vcsIdentifier: "test.xml", submissionId: "m12345")
        Revision rev1 = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(rev1.validate())
        model.addToRevisions(rev1)
        assertTrue(model.validate())
        model.save()
        // user has no right on the model - he is not allowed to revoke read access
        def auth = authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
            modelService.revokeReadAccess(model, User.findByUsername("username"))
        }
        // add rights to testuser
        aclUtilService.addPermission(model, "testuser", BasePermission.ADMINISTRATION)
        aclUtilService.addPermission(model, "testuser", BasePermission.READ)
        // let's try revoking our own right - should not be possible
        assertFalse(modelService.revokeReadAccess(model, User.findByUsername("testuser")))
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.READ))
        // grant read right to user
        modelService.grantReadAccess(model, User.findByUsername("username"))
        def auth2 = authenticateAsUser()
        assertTrue(aclUtilService.hasPermission(auth2, model, BasePermission.READ))
        // and revoke again
        authenticateAsTestUser()
        assertTrue(modelService.revokeReadAccess(model, User.findByUsername("username")))
        assertFalse(aclUtilService.hasPermission(auth2, model, BasePermission.READ))
        // test the same as admin user
        authenticateAsAdmin()
        // testuser is still admin to the model - right should not be revoked
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.READ))
        assertFalse(modelService.revokeReadAccess(model, User.findByUsername("testuser")))
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.READ))
        // remove the admin right and add write right
        aclUtilService.deletePermission(model, "testuser", BasePermission.ADMINISTRATION)
        aclUtilService.addPermission(model, "testuser", BasePermission.WRITE)
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.READ))
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.WRITE))
        // revoke right, should remove both read and write
        assertTrue(modelService.revokeReadAccess(model, User.findByUsername("testuser")))
        assertFalse(aclUtilService.hasPermission(auth, model, BasePermission.READ))
        assertFalse(aclUtilService.hasPermission(auth, model, BasePermission.WRITE))
    }

    @Test
    void testRevokeWriteAccess() {
        Model model = new Model(vcsIdentifier: "test.xml", submissionId: "m123")
        Revision rev1 = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("testuser"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(rev1.validate())
        model.addToRevisions(rev1)
        assertTrue(model.validate())
        model.save()
        // user has no right on the model - he is not allowed to revoke write access
        def auth = authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
            modelService.revokeReadAccess(model, User.findByUsername("username"))
        }
        // add rights to testuser
        aclUtilService.addPermission(model, "testuser", BasePermission.ADMINISTRATION)
        aclUtilService.addPermission(model, "testuser", BasePermission.READ)
        aclUtilService.addPermission(model, "testuser", BasePermission.WRITE)
        // let's try revoking our own right - should not be possible
        assertFalse(modelService.revokeWriteAccess(model, User.findByUsername("testuser")))
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.WRITE))
        // grant write right to user
        modelService.grantReadAccess(model, User.findByUsername("username"))
        modelService.grantWriteAccess(model, User.findByUsername("username"))
        def auth2 = authenticateAsUser()
        assertTrue(aclUtilService.hasPermission(auth2, model, BasePermission.READ))
        assertTrue(aclUtilService.hasPermission(auth2, model, BasePermission.WRITE))
        // and revoke again
        authenticateAsTestUser()
        assertTrue(modelService.revokeWriteAccess(model, User.findByUsername("username")))
        assertFalse(aclUtilService.hasPermission(auth2, model, BasePermission.WRITE))
        // test the same as admin user
        authenticateAsAdmin()
        // testuser is still admin to the model - right should not be revoked
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.WRITE))
        assertFalse(modelService.revokeWriteAccess(model, User.findByUsername("testuser")))
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.WRITE))
        // remove the admin right
        aclUtilService.deletePermission(model, "testuser", BasePermission.ADMINISTRATION)
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.READ))
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.WRITE))
        // revoke right
        assertTrue(modelService.revokeWriteAccess(model, User.findByUsername("testuser")))
        assertFalse(aclUtilService.hasPermission(auth, model, BasePermission.WRITE))
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.READ))
        // calling again should not change anything
        assertTrue(modelService.revokeWriteAccess(model, User.findByUsername("testuser")))
        assertFalse(aclUtilService.hasPermission(auth, model, BasePermission.WRITE))
        assertTrue(aclUtilService.hasPermission(auth, model, BasePermission.READ))
    }

    @Test
    void testDeleteModel() {
        Model model = new Model(vcsIdentifier: "test.xml", submissionId: "m1234")
        Revision revision = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("username"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision.validate())
        model.addToRevisions(revision)
        assertTrue(model.validate())
        model.save()
        // testUser should not get access to the method
        authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
            modelService.deleteModel(model)
        }
        // user should not get access to the method
        authenticateAsUser()
        shouldFail(AccessDeniedException) {
            modelService.deleteModel(model)
        }
        // now user should get access to the method
        final String username = revision.owner.username
        aclUtilService.addPermission(model, username, BasePermission.DELETE)
        assertTrue(modelService.deleteModel(model))
        // set model state back to initial tate
        model.deleted = false
        // admin should get access to the method
        authenticateAsAdmin()
        assertTrue(modelService.deleteModel(model))
        revision.state=ModelState.PUBLISHED
        revision.save(flush:true)
        shouldFail(AccessDeniedException) {
        	modelService.deleteModel(model)
        }
        model = null
        shouldFail(IllegalArgumentException) {
            modelService.deleteModel(model)
        }
    }

    @Test
    void testRestoreModel() {
        Model model = new Model(vcsIdentifier: "test.xml", submissionId: "MODEL1")
        Revision revision = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("username"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision.validate())
        model.addToRevisions(revision)
        assertTrue(model.validate())
        model.save()
        // testUser should not get access to the method
        authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
            modelService.restoreModel(model)
        }
        // user should not get access to the method
        authenticateAsUser()
        shouldFail(AccessDeniedException) {
            modelService.restoreModel(model)
        }
        // admin should get access to the method
        authenticateAsAdmin()
        assertFalse(modelService.restoreModel(model))
        model.deleted = true
        assertTrue(modelService.restoreModel(model))
        revision.state = ModelState.PUBLISHED
        revision.save()
        shouldFail(AccessDeniedException) {
        	modelService.deleteModel(model)
        }
        model = null
        shouldFail(IllegalArgumentException) {
            modelService.deleteModel(model)
        }
    }

    @Test
    void testDeleteRevision() {
        Model model = new Model(vcsIdentifier: "test.xml", submissionId: "MODEL1")
        Revision revision = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("username"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision.validate())
        model.addToRevisions(revision)
        assertTrue(model.validate())
        model.save()
        // testUser should not get access to the method
        authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
            modelService.deleteRevision(revision)
        }
        // Revision should not be set to deleted
        assertFalse(revision.deleted)
        // user should get access to the method
        authenticateAsUser()
        final String username = revision.owner.username
        // let's add the required rights
        aclUtilService.addPermission(model, username, BasePermission.DELETE)
        aclUtilService.addPermission(revision, username, BasePermission.DELETE)
        modelService.deleteRevision(revision)
        assertTrue(revision.deleted)
        // a second time the revision cannot be set to null
        assertFalse(modelService.deleteRevision(revision))
        // Set flag back to not deleted
        revision.deleted = false
        assertFalse(revision.deleted)
        // admin should get access to the method
        authenticateAsAdmin()
        shouldFail(AccessDeniedException) {
        	modelService.deleteRevision(revision)
        }
        assertFalse(revision.deleted)
        // add another revision to test if only current revision is set to deleted
        Revision revision2 = new Revision(model: model, vcsId: "2", revisionNumber: 2, owner: User.findByUsername("username"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision2.validate())
        model.addToRevisions(revision2)
        assertTrue(model.validate())
        model.save()
        // Set flag of first revision back to not deleted
        revision.deleted = false
        authenticateAsAdmin()
        modelService.deleteRevision(revision2)
        assertTrue(revision2.deleted)
        assertFalse(revision.deleted)
        revision2.deleted = false
        // test is it's this time enough to give user the right to delete only the revision
        authenticateAsUser()
        aclUtilService.addPermission(revision2, username, BasePermission.DELETE)
        modelService.deleteRevision(revision2)
    }

    @Test
    void testPublishModelRevision() {
        Model model = new Model(vcsIdentifier: "test.xml", submissionId: "m1")
        Revision revision = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("curator"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision.validate())
        model.addToRevisions(revision)
        assertTrue(model.validate())
        model.save()
        // testUser should not get access to the method
        authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
            modelService.publishModelRevision(revision)
        }
        // user should not get access to the method
        authenticateAsUser()
        shouldFail(AccessDeniedException) {
            modelService.publishModelRevision(revision)
        }
        // user should not see the revision
        authenticateAsUser()
        Revision latest = modelService.getLatestRevision(model)
        assertNull(latest)
        // curator should get access to the method
        authenticateAsCurator()
        aclUtilService.addPermission(Revision.class, revision.id, "ROLE_CURATOR", BasePermission.ADMINISTRATION)
        modelService.publishModelRevision(revision)
        assertNotNull(modelService.getLatestRevision(model))
        // user should now see the revision
        authenticateAsUser()
        assertNotNull(modelService.getLatestRevision(model))
        // Anonymous should now see the revision
        authenticateAnonymous()
        assertNotNull(modelService.getLatestRevision(model))
        // admin should get access to the method
        authenticateAsAdmin()
        modelService.publishModelRevision(revision)
        // null set revision should lead to exception
        revision = null
        shouldFail(IllegalArgumentException) {
            modelService.publishModelRevision(revision)
        }
        revision = new Revision(model: model, vcsId: "1", revisionNumber: 1, owner: User.findByUsername("curator"), minorRevision: false, name:"", description: "", comment: "", uploadDate: new Date(), format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        // deleted revision should lead to exception
        revision.deleted
        shouldFail(IllegalArgumentException) {
            modelService.publishModelRevision(revision)
        }
    }

    @Test
    void funnyPharmMLModelNamesAreOK() {
        def f = new File("test/files/JUM-84/pharmml/testPharmML.xml")
        assertTrue f.exists()
        String name = JummpXmlUtils.findModelElement(f, "Name").trim()
        assertNotNull name
        def rf = new RepositoryFileTransportCommand(path: f.absolutePath, description: "",
                mainFile: true)
        def fmt = new ModelFormatTransportCommand(identifier: "PharmML",
                formatVersion: "0.3.1")
        def mtc = new ModelTransportCommand()
        def rev = new RevisionTransportCommand(name: name, validated: true, format: fmt, model: mtc)
        authenticateAsUser()
        Model m = modelService.uploadValidatedModel([rf], rev)
        String submissionId = m.submissionId
        assertNotNull m
        Revision checkout = modelService.getLatestRevision(m, false)
        assertNotNull checkout
        assertTrue checkout.model.vcsIdentifier.endsWith("$submissionId/")
        assertEquals name, checkout.name
        File vcsFolder = new File(fileSystemService.currentModelContainer).listFiles().find {
            it.isDirectory() && it.name.endsWith("$submissionId")
        }
        assertNotNull vcsFolder
        def checkoutFiles = modelService.vcsService.retrieveFiles(checkout)
        assertEquals 1, checkoutFiles.size()
        String checkoutFilePath = checkoutFiles.first().path
        assertNotNull checkoutFilePath
    }

    @Test
    void funnySbmlModelNamesAreOK() {
        authenticateAsUser()
        def root = new File("test/files/JUM-84/sbml/")
        root.eachFile { f ->
            assertTrue f.exists()
            String name = JummpXmlUtils.findModelAttribute(f, "model", "name").trim()
            assertNotNull name
            def rf = new RepositoryFileTransportCommand(path: f.absolutePath, description: "",
                    mainFile: true)
            def fmt = new ModelFormatTransportCommand(identifier: "SBML",
                    formatVersion: "L2V4")
            def mtc = new ModelTransportCommand()
            def rev = new RevisionTransportCommand(name: name, validated: true, format: fmt,
                    model: mtc)
            Model m = modelService.uploadValidatedModel([rf], rev)
            assertNotNull m
            String submissionId = m.submissionId
            Revision checkout = modelService.getLatestRevision(m, false)
            assertNotNull checkout
            assertTrue checkout.model.vcsIdentifier.endsWith("$submissionId/")
            assertEquals name, checkout.name
            def vcsRoot = new File(fileSystemService.currentModelContainer)
            File vcsFolder = vcsRoot.listFiles().find {
                it.isDirectory() && it.name.endsWith("$submissionId")
            }
            assertNotNull vcsFolder
            def checkoutFiles = modelService.vcsService.retrieveFiles(checkout)
            assertEquals 1, checkoutFiles.size()
            String checkoutFilePath = checkoutFiles.first().path
            assertNotNull checkoutFilePath
        }
    }
}
