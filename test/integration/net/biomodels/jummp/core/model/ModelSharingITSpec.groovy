/*
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
import net.biomodels.jummp.core.adapters.RevisionAdapter
import net.biomodels.jummp.core.util.JummpXmlUtils
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.User
import org.apache.commons.io.FileUtils
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.model.AccessControlEntry
import org.springframework.security.acls.model.Acl
import org.springframework.security.acls.model.Permission
import org.springframework.transaction.TransactionDefinition

import java.nio.file.Path
import java.nio.file.Paths

import static org.springframework.security.acls.domain.BasePermission.*

@FreshRuntime
class ModelSharingITSpec extends IntegrationSpec {
    def fileSystemService
    def grailsApplication
    JummpIntegrationTest jummpIntegrationTest
    def vcsService
    def miriamService
    def modelService
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

        def mtc = new ModelTransportCommand()
        def rev = new RevisionTransportCommand(name: name, validated: true, format: fmt, model: mtc, owner: "username")
        Model m = modelService.uploadValidatedModel([rf], rev)
        Model.withSession { s ->
            s.flush()
            s.clear()
        }
        then: "the model was saved successfully"
        m.id

        when:
        File[] additionalFilesPool = new File('test/files/JUM-84/sbml/').listFiles()
        then:
        additionalFilesPool.length > 4 // need to insert 4 more revisions and then submit for publication

        when: "we insert a new revision"
        def baseRev = new RevisionAdapter(revision: m.revisions.first()).toCommandObject()
        def rf2 = new RepositoryFileTransportCommand(path: additionalFilesPool[0].absolutePath, mainFile: false, description: 'additional file 1')
        baseRev.comment = "revision 2: $rf2"
        def r2 = modelService.addRevision([rf, rf2], [], baseRev)
        then: "it is valid"
        r2
        r2.validate()
        r2 instanceof Revision
        r2.repoFiles.findAll { !it.mainFile }.size() == 1

        when:
        m = Model.get(m.id)
        Revision r1 = m.revisions.find({it.revisionNumber == 1})
        then:
        m.revisions.size() == 2

        when:
        modelService.submitModelRevisionForPublication(r2)
        then:
        noExceptionThrown()

        when:
        def myModels = modelService.getAllModels(0, 10, false, ModelListSorting.LAST_MODIFIED, "type:Private")
        def aclUtilService = grailsApplication.mainContext.aclUtilService
        Acl modelPermissions = aclUtilService.readAcl(m)
        Acl r1Perms = aclUtilService.readAcl r1
        Acl r2Perms = aclUtilService.readAcl r2

        then:
        myModels.size() == 1
        // myModels is a list of proxies because we're outside of the transaction,
        // so we can only compare ids
        myModels.first().id == m.id

        and: 'the user has READ, WRITE, DELETE, ADMIN permissions on the model'
        filterModelOwnerPermissions(modelPermissions).size() == 4

        and: 'the user has read, delete, admin permissions on the revisions'
        filterRevisionOwnerPermissions(r1Perms).size() == 3
        // we create duplicate ACL entries when updating models
        filterRevisionOwnerPermissions(r2Perms).size() >= 3
    }

    private static List<AccessControlEntry> filterModelOwnerPermissions(Acl modelPermissions) {
        filterOwnerPermissions modelPermissions, [READ, WRITE, DELETE, ADMINISTRATION]
    }

    private static List<AccessControlEntry> filterRevisionOwnerPermissions(Acl r1Perms) {
        filterOwnerPermissions r1Perms, [READ, DELETE, ADMINISTRATION]
    }

    private static List<AccessControlEntry> filterOwnerPermissions(Acl acl, List<? extends Permission> permissions) {
        acl.entries.findAll { AccessControlEntry aclEntry ->
            isOwnerPermission(aclEntry) && (aclEntry.permission in permissions)
        }
    }

    private static boolean isOwnerPermission(AccessControlEntry aclEntry) {
        (aclEntry.sid instanceof PrincipalSid) && (((PrincipalSid) aclEntry.sid).principal == "username")
    }
}
