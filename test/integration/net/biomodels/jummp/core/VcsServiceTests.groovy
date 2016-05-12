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
* JGit, Apache Commons, Svnkit, Spring Framework, JUnit, 
* Spring Security (or a modified version of that library), containing parts
* covered by the terms of Common Public License, Apache License v2.0, 
* TMate Open Source License, Eclipse Distribution License v1.0, the licensors of
* this Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of JGit, Apache Commons, Svnkit, 
* Spring Framework, JUnit, Spring Security used as well as that of the 
* covered work.}
**/





package net.biomodels.jummp.core

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import net.biomodels.jummp.core.vcs.FileNotVersionedException
import net.biomodels.jummp.core.vcs.VcsException
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.git.GitManagerFactory
import net.biomodels.jummp.plugins.security.User
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.*
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.mock.web.MockServletContext
import org.springframework.security.access.AccessDeniedException
//import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import static org.junit.Assert.*

@TestMixin(IntegrationTestMixin)
class VcsServiceTests extends JummpIntegrationTest implements ApplicationContextAware {
    /**
     * Dependency injection of the service we want to test
     */
    def vcsService
    /**
     * Dependency injection of ModelService for easier manipulation
     */
    def modelService
    /**
     * Dependency infection of application context
     */
    def appCtx
    /**
     * Dependency injection of grails Application
     */
    def grailsApplication
    /**
     * Dependency injection of fileSystemService
     */
    def fileSystemService

    void setApplicationContext(ApplicationContext applicationContext) {
        appCtx = applicationContext
    }

    @Before
    void setUp() {
        createUserAndRoles()
        vcsService.vcsManager = null
        def exchangeDir = new File("target/vcs/exchange/")
        assertTrue(exchangeDir.mkdirs())
    }

    @After
    void tearDown() {
        FileUtils.deleteDirectory(new File("target/vcs/vvv"))
        FileUtils.deleteDirectory(new File("target/vcs/git"))
        FileUtils.deleteDirectory(new File("target/vcs/repository"))
        FileUtils.deleteDirectory(new File("target/vcs/exchange"))
        vcsService.vcsManager = null
        appCtx.getBean("gitManagerFactory").git = null
    }

    @Test
    void testGit() {
        // verifies that the service is valid, if git backend is configured correctly
        grailsApplication.config.jummp.vcs.pluginServiceName="gitManagerFactory"
        grailsApplication.config.jummp.plugins.git.enabled=true
        grailsApplication.config.jummp.vcs.workingDirectory="target/vcs/"
        File root = new File("target/vcs/")
        String containerPath = root.absolutePath + "/vvv/"
        fileSystemService.currentModelContainer = containerPath
        vcsService.modelContainerRoot = grailsApplication.config.jummp.vcs.workingDirectory
        File gitDirectory = new File("target/vcs/vvv/git/")
        gitDirectory.mkdirs()

        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        Repository repository = builder.setWorkTree(gitDirectory)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()
        Git git = new Git(repository)
        git.init().setDirectory(gitDirectory).call()
        assertFalse(vcsService.isValid())
        vcsService.vcsManager = appCtx.getBean("gitManagerFactory").getInstance()
        assertTrue(vcsService.isValid())
    }

    @Ignore("Don't test SVN because it does not reflect the changes made to VcsManager")
    @Test
    void testSvn() {
        // verifies that the service is valid, if svn backend is configured correctly
        grailsApplication.config.jummp.vcs.pluginServiceName = "svnManagerFactory"
        grailsApplication.config.jummp.plugins.subversion.enabled = true
        grailsApplication.config.jummp.plugins.subversion.localRepository = "target/vcs/repository/"
        SVNRepositoryFactory.createLocalRepository(new File("target/vcs/repository/"), true, false)
        assertFalse(vcsService.isValid())
        vcsService.vcsManager = appCtx.getBean("svnManagerFactory").getInstance()
        assertTrue(vcsService.isValid())
    }

    @Test
    void testImport() {
        String modelIdentifier = "test/"
        fileSystemService.root = new File("target/vcs/git").canonicalFile
        String containerPath = fileSystemService.root.absolutePath + "/aaa/"
        fileSystemService.currentModelContainer = containerPath
        //modelService ensures that the model folder gets created
        File modelDirectory = new File(new File(containerPath), "test")
        modelDirectory.mkdirs()
        vcsService.modelContainerRoot = "target/vcs/git"
        assertFalse(vcsService.isValid())
        // first create a model
        Model model = new Model(vcsIdentifier: "/aaa/${modelIdentifier}", submissionId: "MODEL001")
        Revision revision = new Revision(model: model, vcsId: "1", revisionNumber: 1,
                owner: User.findByUsername("testuser"), minorRevision: false, name: "test",
                description:"", comment: "", uploadDate: new Date(),
                format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision.validate())
        model.addToRevisions(revision)
        assertTrue(model.validate())
        model.save()
        // without authentication it should fail
        authenticateAnonymous()
        shouldFail(AccessDeniedException) {
            vcsService.importModel(model, null)
        }
        // as user we should get a VcsException as the service is not valid
        authenticateAsUser()
        shouldFail(VcsException) {
            vcsService.importModel(model, null)
        }
        // same of course for admin
        authenticateAsAdmin()
        shouldFail(VcsException) {
            vcsService.importModel(model, null)
        }
        // create a git repository
        List<File> imports=new LinkedList<File>();
        for (i in 0..9) {
            imports.add(new File("target/vcs/exchange/test${i}.xml"));
            FileUtils.touch(imports.get(i));
            imports.get(i).append("Test - ${i}\n");
        }
        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        grailsApplication.config.jummp.vcs.workingDirectory = "target/vcs/git"
        grailsApplication.config.jummp.vcs.exchangeDirectory = "target/vcs/exchange"
        vcsService.vcsManager = gitService.getInstance()
        vcsService.vcsManager.exchangeDirectory = new File("target/vcs/exchange")
        vcsService.modelContainerRoot = grailsApplication.config.jummp.vcs.workingDirectory
        assertTrue(vcsService.isValid())
        // now as user we should be able to import
        authenticateAsTestUser()
        String rev = vcsService.importModel(model, imports)

        for (i in 0..9) {
            File gitFile = new File(modelDirectory, "test${i}.xml")
            List<String> lines = gitFile.readLines()
            assertEquals(1, lines.size())
            assertEquals("Test - ${i}".toString(), lines[0])
        }

        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        Repository repository = builder.setWorkTree(modelDirectory)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir(modelDirectory) // scan up the file system tree
                .build()
        // ensure the revision and commit message is correct
        ObjectId commit = repository.resolve(Constants.HEAD)
        RevWalk revWalk = new RevWalk(repository)
        RevCommit revCommit = revWalk.parseCommit(commit)
        assertEquals(commit.getName(), rev)
        // we did not specify a commit message, so default should be used
        assertTrue(revCommit.getShortMessage().contains("Imported model at"))
        assertTrue(revCommit.getFullMessage().contains("Imported model at"))
        // importing again should fail
/*        shouldFail(VcsException) {
            vcsService.importFile(model, importFile)
        }*/
    }

    @Test
    void testUpdate() {
        fileSystemService.root = new File("target/vcs/git").canonicalFile
        String containerPath = fileSystemService.root.absolutePath + "/uuu/"
        fileSystemService.currentModelContainer = containerPath
        //modelService ensures that the model folder gets created
        File modelDirectory = new File(new File(containerPath), "testUpdate")
        modelDirectory.mkdirs()
        vcsService.modelContainerRoot = "target/vcs/git"

        assertFalse(vcsService.isValid())
        // first create a model
        String modelIdentifier="testUpdate/"
        assertFalse(vcsService.isValid())
        // first create a model
        Model model = new Model(vcsIdentifier: "/uuu/"+modelIdentifier, submissionId: "MODEL001")
        Revision revision = new Revision(model: model, vcsId: "1", revisionNumber: 1,
                owner: User.findByUsername("testuser"), minorRevision: false, name: "test",
                description: "", comment: "", uploadDate: new Date(),
                format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision.validate())
        model.addToRevisions(revision)
        assertTrue(model.validate())
        model.save()

        authenticateAnonymous()
        shouldFail(AccessDeniedException) {
            vcsService.updateModel(model, null, null, null)
        }
        // as user we should get an access denied exception
        authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
            vcsService.updateModel(model, null, null, null)
        }
        // as admin we should get an VcsException as the vcsService is not valid
        authenticateAsAdmin()
        shouldFail(VcsException) {
            vcsService.updateModel(model, null, null, null)
        }
        // now also the user should get a VcsException
        modelService.grantWriteAccess(model, User.findByUsername("testuser"))
        authenticateAsTestUser()
        shouldFail(VcsException) {
            vcsService.updateModel(model, null, null, null)
        }

        List<File> imports=new LinkedList<File>();
        for (i in 0..9) {
            imports.add(new File("target/vcs/exchange/test${i}.xml"));
            FileUtils.touch(imports.get(i));
            imports.get(i).append("Test - ${i}\n");
        }
        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        grailsApplication.config.jummp.vcs.exchangeDirectory = "target/vcs/exchange"
        grailsApplication.config.jummp.vcs.workingDirectory = "target/vcs/git"
        vcsService.modelContainerRoot = grailsApplication.config.jummp.vcs.workingDirectory
        vcsService.vcsManager = gitService.getInstance()
        assertTrue(vcsService.isValid())

        String rev = vcsService.updateModel(model, imports, null, null)
        for (i in 0..9) {
            File gitFile = new File(modelDirectory, "test${i}.xml")
            List<String> lines = gitFile.readLines()
            assertEquals(1, lines.size())
            assertEquals("Test - ${i}".toString(), lines[0])
        }

        // setup VCS
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        Repository repository = builder.setWorkTree(modelDirectory)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir(modelDirectory) // scan up the file system tree
                .build()
        // ensure the revision and commit message is correct
        ObjectId commit = repository.resolve(Constants.HEAD)
        RevWalk revWalk = new RevWalk(repository)
        RevCommit revCommit = revWalk.parseCommit(commit)
        assertEquals(commit.getName(), rev)
        // we passed null as commit message, so default should be used
        assertTrue(revCommit.getShortMessage().contains("Updated at"))
        assertTrue(revCommit.getFullMessage().contains("Updated at"))
        // try again with an empty commit message - should also be default message
        imports.each {
            it.append("Second Test\n")
        }
        rev = vcsService.updateModel(model, imports, null, null)

        for (i in 0..9) {
            File gitFile = new File(modelDirectory, "test${i}.xml")
            List<String> lines = gitFile.readLines()
            assertEquals(2, lines.size())
            assertEquals("Test - ${i}".toString(), lines[0])
            assertEquals("Second Test", lines[1])
        }

        repository = builder.setWorkTree(modelDirectory)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir(modelDirectory) // scan up the file system tree
                .build()
        commit = repository.resolve(Constants.HEAD)
        revWalk = new RevWalk(repository)
        revCommit = revWalk.parseCommit(commit)
        assertEquals(commit.getName(), rev)
        assertTrue(revCommit.getShortMessage().contains("Updated at"))
        assertTrue(revCommit.getFullMessage().contains("Updated at"))
        // try with a custom commit message

        imports.each {
            it.append("Third Test\n")
        }
        rev = vcsService.updateModel(model, imports, null, "Commit Message")

        for (i in 0..9) {
            File gitFile = new File(modelDirectory, "test${i}.xml")
            List<String> lines = gitFile.readLines()
            assertEquals(3, lines.size())
            assertEquals("Test - ${i}".toString(), lines[0])
            assertEquals("Second Test", lines[1])
            assertEquals("Third Test", lines[2])
        }

        repository = builder.setWorkTree(modelDirectory)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir(modelDirectory) // scan up the file system tree
                .build()
        commit = repository.resolve(Constants.HEAD)
        revWalk = new RevWalk(repository)
        revCommit = revWalk.parseCommit(commit)
        assertEquals(commit.getName(), rev)
        assertEquals("Commit Message", revCommit.getShortMessage())
        assertEquals("Commit Message", revCommit.getFullMessage())
        // and finally verify as admin
        authenticateAsAdmin()

        imports.each {
            it.append("Admin Test\n")
        }

        rev = vcsService.updateModel(model, imports, null, "Admin Commit Message")

        for (i in 0..9) {
            File gitFile = new File(modelDirectory, "test${i}.xml")
            List<String> lines = gitFile.readLines()
            assertEquals(4, lines.size())
            assertEquals("Test - ${i}".toString(), lines[0])
            assertEquals("Second Test", lines[1])
            assertEquals("Third Test", lines[2])
            assertEquals("Admin Test", lines[3])
        }

        repository = builder.setWorkTree(modelDirectory)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir(modelDirectory) // scan up the file system tree
                .build()
        commit = repository.resolve(Constants.HEAD)
        revWalk = new RevWalk(repository)
        revCommit = revWalk.parseCommit(commit)
        assertEquals(commit.getName(), rev)
        assertEquals("Admin Commit Message", revCommit.getShortMessage())
        assertEquals("Admin Commit Message", revCommit.getFullMessage())

        imports.each {
            it.append("Inexistent delete test\n")
        }
        def deletes = [new File("/tmp/inexistent")]
        rev = vcsService.updateModel(model, imports, deletes, "Deleted inexistent file.")
        for (i in 0..9) {
            File gitFile = new File(modelDirectory, "test${i}.xml")
            List<String> lines = gitFile.readLines()
            assertEquals(5, lines.size())
            assertEquals("Test - ${i}".toString(), lines[0])
            assertEquals("Second Test", lines[1])
            assertEquals("Third Test", lines[2])
            assertEquals("Admin Test", lines[3])
            assertEquals("Inexistent delete test", lines[4])
        }
        repository = builder.setWorkTree(modelDirectory)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir(modelDirectory) // scan up the file system tree
                .build()
        commit = repository.resolve(Constants.HEAD)
        revWalk = new RevWalk(repository)
        revCommit = revWalk.parseCommit(commit)
        assertEquals(commit.getName(), rev)
        assertEquals("Deleted inexistent file.", revCommit.getShortMessage())
        assertEquals("Deleted inexistent file.", revCommit.getFullMessage())

        // don't make deletes and imports disjoint - MWUHAHAHAHAAA!
        deletes = [*3..9].collect{ new File(modelDirectory, "test${it}.xml") }
        deletes.each { assertTrue it.exists() }
        imports.each {
            it.append("Testing the deletion of actual files.\n")
        }
        rev = vcsService.updateModel(model, imports, deletes, "Deleted actual files.")
        deletes.each { assertFalse it.exists() }
        def delta = imports - deletes
        delta.each {
            assertTrue it.exists()
            String i = it.name - "test" - ".xml"
            List<String> lines = it.readLines()
            assertEquals(6, lines.size())
            assertEquals("Test - ${i}".toString(), lines[0])
            assertEquals("Second Test", lines[1])
            assertEquals("Third Test", lines[2])
            assertEquals("Admin Test", lines[3])
            assertEquals("Inexistent delete test", lines[4])
            assertEquals("Testing the deletion of actual files.", lines[5])
        }
        commit = repository.resolve(Constants.HEAD)
        revWalk = new RevWalk(repository)
        revCommit = revWalk.parseCommit(commit)
        assertEquals(commit.getName(), rev)
        assertEquals("Deleted actual files.", revCommit.getShortMessage())
        assertEquals("Deleted actual files.", revCommit.getFullMessage())

        // and last but not least user should still get an AccessDeniedException
        authenticateAsUser()
        shouldFail(AccessDeniedException) {
            vcsService.updateModel(model, imports, null, "Commit Message")
        }
    }

    @Test
    void testRetrieve() {
        fileSystemService.root = new File("target/vcs/git").canonicalFile
        String containerPath = fileSystemService.root.absolutePath + "/aaa/"
        fileSystemService.currentModelContainer = containerPath
        vcsService.modelContainerRoot = "target/vcs/git"

        assertFalse(vcsService.isValid())
        String modelIdentifier="modelid"
        //modelService ensures that the model folder gets created
        File modelDirectory = new File(new File(containerPath), modelIdentifier)
        modelDirectory.mkdirs()
        assertFalse(vcsService.isValid())
        // first create a model
        Model model = new Model(vcsIdentifier: "aaa/${modelIdentifier}", submissionId: "MODEL001")
        Revision revision = new Revision(model: model, vcsId: "1", revisionNumber: 1,
                owner: User.findByUsername("testuser"), minorRevision: false, name: "test",
                description: "", comment: "", uploadDate: new Date(),
                format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        assertTrue(revision.validate())
        model.addToRevisions(revision)
        assertTrue(model.validate())
        model.save()
        // without authentication it should fail
        authenticateAnonymous()
        shouldFail(AccessDeniedException) {
            vcsService.retrieveFiles(revision)
        }
        // as user it should fail - no ACL yet
        authenticateAsTestUser()
        shouldFail(AccessDeniedException) {
            vcsService.retrieveFiles(revision)
        }
        // as admin it should fail with a VcsException cause the VCS is not yet valid
        authenticateAsAdmin()
        shouldFail(VcsException) {
            vcsService.retrieveFiles(revision)
        }
        modelService.grantReadAccess(model, User.findByUsername("testuser"))
        // as testuser it should fail now with a VcsException instead of AccessDeniedException
        authenticateAsTestUser()
        shouldFail(VcsException) {
            vcsService.retrieveFiles(revision)
        }
        // create a git repository
        List<File> imports=new LinkedList<File>();
        List<Integer> numbers=new LinkedList<Integer>();
        for (i in 0..9) {
            imports.add(new File("target/vcs/exchange/test${i}.xml"));
            FileUtils.touch(imports.get(i));
            imports.get(i).append("${i}\n");
            numbers.add("${i}".toString());
        }
        // setup VCS
        File clone = new File(model.vcsIdentifier)
        clone.mkdirs()

        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        Repository repository = builder.setWorkTree(clone)
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build()

        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        grailsApplication.config.jummp.vcs.workingDirectory = "target/vcs/git"
        grailsApplication.config.jummp.vcs.exchangeDirectory = "target/vcs/exchange"
        vcsService.modelContainerRoot = grailsApplication.config.jummp.vcs.workingDirectory
        vcsService.vcsManager = gitService.getInstance()
        assertTrue(vcsService.isValid())
        // git is valid, but file does not yet exist
        shouldFail(VcsException) {
            vcsService.retrieveFiles(revision)
        }

        revision.vcsId = vcsService.importModel(model, imports)
        revision.save(flush: true)
        List<File> exchangeFiles = vcsService.retrieveFiles(revision)
        exchangeFiles.each { exchange ->
            imports.each { imported ->
                 assertNotSame(exchange, imported)
            }
            List<String> lines = exchange.readLines()
            assertEquals(1, lines.size())
            numbers.remove(lines.first());
        }
        assertTrue(numbers.isEmpty());
    }
}
