/**
* Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
* JGit, Apache Commons, Grails (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, Eclipse Distribution License v1.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of JGit, Apache Commons, Grails used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.plugins.git

import grails.test.GrailsUnitTestCase
import net.biomodels.jummp.core.vcs.InvalidVcsRepositoryException
import net.biomodels.jummp.core.vcs.VcsAlreadyInitedException
import net.biomodels.jummp.core.vcs.VcsException
import net.biomodels.jummp.core.vcs.VcsFileDetails
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.junit.Test

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.junit.Assert.assertNotEquals

class GitManagerTests extends GrailsUnitTestCase {
    private File clone
    private File exchangeDirectory
    private GitManager gitManager
    private Git git
    private Repository repository

    protected void setUp() {
        super.setUp()
        clone = new File("target/vcs/clone")
        clone.mkdirs()
        exchangeDirectory = new File("target/vcs/exchange")
        exchangeDirectory.mkdirs()
        gitManager = new GitManager()
        repository = GitSupport.buildRepository(clone)
        git = new Git(repository)
        git.init().setDirectory(clone).call()
    }

    protected void tearDown() {
        super.tearDown()
        FileUtils.deleteDirectory(new File("target/vcs/"))
    }

    void testInit() {
        println ">> Running testInit()"
        shouldFail VcsException, {
            // test that we cannot init into a file
            File noDirectory = new File("target/vcs/tmp")
            FileUtils.touch(noDirectory)
            gitManager.init(noDirectory)
        }

        shouldFail VcsException, {
            // test that we cannot initialise into a non-existing directory
            gitManager.init(new File("target/vcs/tmp2"))
        }

        // The init method should work now
        gitManager.init(exchangeDirectory)
        shouldFail VcsAlreadyInitedException, {
            // test that we cannot initialise the checkout twice
            gitManager.init(exchangeDirectory)
        }
    }

    @Test
    void testGetFileDetails() {
        shouldFail IOException, {
            // the 'clone' directory has been created and initialised without any commit
            gitManager.getFileDetails(clone, ".git")
        }

        gitManager.init(clone)
        // import the first file
        File firstFile = new File(clone, "test.txt")
        firstFile.append("This is the test file")
        FileUtils.touch(firstFile)
        GitSupport.importFile(git, firstFile.name)
        String authorName = "Admin"
        String authorEmail = "admin@example.com"
        String commitMessage = "Upload the first files"
        GitSupport.makeCommit(git, authorName, authorEmail, commitMessage)

        List<VcsFileDetails> details = gitManager.getFileDetails(clone, "test.txt")
        assertNotNull(details)
        assertEquals(1, details.size())
        VcsFileDetails vcsFileDetails = details.first()
        assertEquals(commitMessage, vcsFileDetails.msg)
        String commitId = git.getRepository().resolve(Constants.HEAD).name
        assertEquals(commitId, vcsFileDetails.revisionId)
    }

    @Test
    void testResetModelDirectory() {
        gitManager.init(clone)
        shouldFail Exception, {
            // commitId does not exist
            gitManager.resetModelRepository(clone, "aaaabbbb")
        }
        // make the first commit
        File firstFile = new File(clone, "test.txt")
        firstFile.append("This is the test file")
        FileUtils.touch(firstFile)
        GitSupport.importFile(git, firstFile.name)
        String authorName = "Admin"
        String authorEmail = "admin@example.com"
        String commitMessage = "Upload the first files"
        GitSupport.makeCommit(git, authorName, authorEmail, commitMessage)

        // make the second commit
        File secondFile = new File(clone, "raw.dat")
        secondFile.append("001 002 003")
        FileUtils.touch(secondFile)
        GitSupport.importFile(git, secondFile.name)
        commitMessage = "Add the data file"
        GitSupport.makeCommit(git, authorName, authorEmail, commitMessage)

        // make the third commit
        secondFile.append("004 005 006")
        FileUtils.touch(secondFile)
        GitSupport.importFile(git, secondFile.name)
        commitMessage = "Update the data file"
        GitSupport.makeCommit(git, authorName, authorEmail, commitMessage)
        String thirdCommitHashId = git.getRepository().resolve(Constants.HEAD).name

        // mame the fourth commit: add another new file
        File thirdFile = new File(clone, "importData.bak")
        thirdFile.text = "This is used to back up data"
        FileUtils.touch(thirdFile)
        GitSupport.importFile(git, thirdFile.name)
        commitMessage = "Upload backup file"
        GitSupport.makeCommit(git, authorName, authorEmail, commitMessage)
        ObjectId head = git.repository.resolve(Constants.HEAD)

        RevWalk revWalk = new RevWalk(git.repository)
        RevCommit revCommit = revWalk.parseCommit(head)
        List<String> files = GitSupport.lsFiles(git, revCommit)
        assertEquals(3, files.size())

        // reset hard to the third commit
        gitManager.resetModelRepository(clone, thirdCommitHashId)
        head = git.repository.resolve(Constants.HEAD)
        assertEquals(thirdCommitHashId, head.name)
        revCommit = revWalk.parseCommit(head)
        files = GitSupport.lsFiles(git, revCommit)
        assertEquals(2, files.size())
    }

    @Test
    void testDeleteCommit() {
        println ">> Running testDeleteCommit()"
        gitManager.init(clone)
        String authorName = "Admin"
        String authorEmail = "admin@example.com"

        // commit 1: fileA.txt
        File fileA = new File(clone, "fileA.txt")
        fileA.text = "fileA v1"
        FileUtils.touch(fileA)
        GitSupport.importFile(git, fileA.name)
        GitSupport.makeCommit(git, authorName, authorEmail, "Add fileA")

        // commit 2: fileB.txt - this is the minor revision we are going to delete
        File fileB = new File(clone, "fileB.txt")
        fileB.text = "fileB v1"
        FileUtils.touch(fileB)
        GitSupport.importFile(git, fileB.name)
        String secondCommitHash = GitSupport.makeCommit(git, authorName, authorEmail,
            "Add fileB (minor revision)").name

        // commit 3: fileC.txt
        File fileC = new File(clone, "fileC.txt")
        fileC.text = "fileC v1"
        FileUtils.touch(fileC)
        GitSupport.importFile(git, fileC.name)
        String thirdCommitHash = GitSupport.makeCommit(git, authorName, authorEmail, "Add fileC").name

        // commit 4: fileD.txt
        File fileD = new File(clone, "fileD.txt")
        fileD.text = "fileD v1"
        FileUtils.touch(fileD)
        GitSupport.importFile(git, fileD.name)
        String fourthCommitHash = GitSupport.makeCommit(git, authorName, authorEmail, "Add fileD").name

        Map<String, String> shaMapping = gitManager.deleteCommit(clone, secondCommitHash)

        // the two commits after the deleted one should have been replayed under new ids
        assertEquals(2, shaMapping.size())
        assertTrue(shaMapping.containsKey(thirdCommitHash))
        assertTrue(shaMapping.containsKey(fourthCommitHash))
        assertNotEquals(thirdCommitHash, shaMapping[thirdCommitHash])
        assertNotEquals(fourthCommitHash, shaMapping[fourthCommitHash])

        RevWalk revWalk = new RevWalk(repository)
        ObjectId head = repository.resolve(Constants.HEAD)
        RevCommit newHead = revWalk.parseCommit(head)
        assertEquals(shaMapping[fourthCommitHash], newHead.name)

        RevCommit newThird = revWalk.parseCommit(newHead.parents[0])
        assertEquals(shaMapping[thirdCommitHash], newThird.name)

        // the replayed third commit's parent is now the first commit: fileB never existed
        RevCommit newFirst = revWalk.parseCommit(newThird.parents[0])
        assertEquals(0, newFirst.parentCount)

        // resulting history has fileA, fileC and fileD, but not fileB
        List<String> files = GitSupport.lsFiles(git, newHead)
        assertEquals(3, files.size())
        assertTrue(files.contains("fileA.txt"))
        assertTrue(files.contains("fileC.txt"))
        assertTrue(files.contains("fileD.txt"))
        assertFalse(files.contains("fileB.txt"))

        // the deleted commit no longer exists in history, so deleting it again must fail
        shouldFail VcsException, {
            gitManager.deleteCommit(clone, secondCommitHash)
        }

        // there is nothing to reparent the first commit onto
        shouldFail VcsException, {
            gitManager.deleteCommit(clone, newFirst.name)
        }
    }

    // regression test for the case where a replayed commit's own diff is empty (it changed
    // nothing relative to its original parent) - JGit's cherry-pick then skips creating a
    // commit and just leaves the tip where it was, which used to make deleteCommit map two
    // different revisions onto the very same sha, silently violating Revision.vcsId's
    // unique:'model' constraint when ModelService tried to persist the remap.
    @Test
    void testDeleteCommitWithEmptyReplay() {
        println ">> Running testDeleteCommitWithEmptyReplay()"
        gitManager.init(clone)
        String authorName = "Admin"
        String authorEmail = "admin@example.com"

        // commit 1: fileA.txt
        File fileA = new File(clone, "fileA.txt")
        fileA.text = "fileA v1"
        FileUtils.touch(fileA)
        GitSupport.importFile(git, fileA.name)
        GitSupport.makeCommit(git, authorName, authorEmail, "Add fileA")

        // commit 2: fileB.txt - this is the minor revision we are going to delete
        File fileB = new File(clone, "fileB.txt")
        fileB.text = "fileB v1"
        FileUtils.touch(fileB)
        GitSupport.importFile(git, fileB.name)
        String secondCommitHash = GitSupport.makeCommit(git, authorName, authorEmail,
            "Add fileB (minor revision)").name

        // commit 3: fileC.txt
        File fileC = new File(clone, "fileC.txt")
        fileC.text = "fileC v1"
        FileUtils.touch(fileC)
        GitSupport.importFile(git, fileC.name)
        String thirdCommitHash = GitSupport.makeCommit(git, authorName, authorEmail, "Add fileC").name

        // commit 4: no file changes at all - e.g. a resubmission that only touched metadata
        String fourthCommitHash = git.commit()
            .setAuthor(authorName, authorEmail)
            .setMessage("Resubmit with no file changes")
            .setAllowEmpty(true)
            .call().name

        Map<String, String> shaMapping = gitManager.deleteCommit(clone, secondCommitHash)

        assertEquals(2, shaMapping.size())
        assertTrue(shaMapping.containsKey(thirdCommitHash))
        assertTrue(shaMapping.containsKey(fourthCommitHash))
        // the critical assertion: even though replaying the fourth commit was a no-op change
        // wise, it must still land on its own distinct sha rather than collapsing onto the
        // replayed third commit's sha
        assertNotEquals(shaMapping[thirdCommitHash], shaMapping[fourthCommitHash])

        RevWalk revWalk = new RevWalk(repository)
        ObjectId head = repository.resolve(Constants.HEAD)
        RevCommit newHead = revWalk.parseCommit(head)
        assertEquals(shaMapping[fourthCommitHash], newHead.name)
        assertEquals("Resubmit with no file changes", newHead.fullMessage)

        RevCommit newThird = revWalk.parseCommit(newHead.parents[0])
        assertEquals(shaMapping[thirdCommitHash], newThird.name)
        // the forced empty commit must carry the same tree as its parent - still no fileB
        assertEquals(newThird.tree.name, newHead.tree.name)

        List<String> files = GitSupport.lsFiles(git, newHead)
        assertEquals(2, files.size())
        assertTrue(files.contains("fileA.txt"))
        assertTrue(files.contains("fileC.txt"))
        assertFalse(files.contains("fileB.txt"))
    }

    @Test
    void testRetrieveModel() {
        println ">> Running testRetrieveModel()"
        shouldFail VcsException, {
            // exchange directory has not been initialised
            gitManager.retrieveModel(new File("/tmp"), "tmp")
        }

        gitManager.init(exchangeDirectory)

        shouldFail VcsException, {
            // non existing file
            gitManager.retrieveModel(new File("target/vcs/tmp"), "test")
        }

        shouldFail Exception, {
            // directory instead of file
            File directory = new File("target/vcs/tmp")
            directory.mkdirs()
            gitManager.retrieveModel(directory, "test")
        }

        File importFile = new File("target/vcs/tmp/test")
        FileUtils.touch(importFile)
        shouldFail InvalidVcsRepositoryException, {
            // it is an invalid git repository
            gitManager.retrieveModel(importFile, "test")
        }

        // test the git repository
        assertNotNull(repository)
        assertTrue(!repository.isBare())
        assertNotNull(gitManager)

        // import the file
        importFile = new File(clone, "test.txt")
        importFile.append("Import the file")
        FileUtils.touch(importFile)
        GitSupport.importFile(git, importFile.name)
        String authorName = "Admin"
        String authorEmail = "admin@example.com"
        String commitMessage = "Upload the first files"
        GitSupport.makeCommit(git, authorName, authorEmail, commitMessage)

        String firstCommitHash = GitSupport.getLatestCommitHashString(repository) // get that for the future use
        String actualCtMsg = GitSupport.getFullLatestCommitMessage(repository)
        assertEquals(commitMessage, actualCtMsg)

        List files = gitManager.retrieveModel(clone)
        assertEquals(1, files.size())

        // now update the repository by adding a new file
        File anotherFile = new File(clone, "dummy.txt")
        anotherFile.append("This is a dummy file")
        GitSupport.importFile(git, anotherFile.name)
        commitMessage = "Add a dummy file"
        GitSupport.makeCommit(git, authorName, authorEmail, commitMessage)
        files = gitManager.retrieveModel(clone)
        assertEquals(2, files.size())

        // test with a specific starting point (i.e. commit hash string, branch name, tag name, etc.)
        // For this case, we test with the first commit
        files = gitManager.retrieveModel(clone, firstCommitHash)
        assertEquals(1, files.size())
    }

    @Test
    void testUpdateModel() {
        assert gitManager != null
        assertEquals(clone.listFiles().size(), 1) // .git directory
        // make the first commit
        List addFiles = []
        List removeFiles = []
        Path tmpGitTestDir
        tmpGitTestDir = Files.createTempDirectory("gitTest1")
        File MODEL001 = new File(tmpGitTestDir.toString(), "MODEL001.txt")
        MODEL001.text = "This is the first commit for MODEL001"
        addFiles << MODEL001
        String ciMsg = "Make the first commit"
        gitManager.updateModel(git, addFiles, removeFiles, ciMsg)
        assertEquals(clone.listFiles().size(), 2) // .git and MODEL001.txt
        println "After making the first commit:"
        clone.listFiles().each {
            println "Item: ${it.getName()}"
        }
        // make the second commit
        /**
         * create a file having the identical name to MODEL001.txt
         * under /tmp
         */
        tmpGitTestDir = Files.createTempDirectory("gitTest2")
        assert Files.isDirectory(tmpGitTestDir)
        Path newMODEL001 = Paths.get(tmpGitTestDir.toString(), "MODEL001.txt")
        String data = "This is the second commit for MODEL001"
        Files.write(newMODEL001, data.getBytes())
        data = "123 456 789"
        Path dat = Paths.get(tmpGitTestDir.toString(), "simulation.dat")
        Files.write(dat, data.getBytes())

        /**
         * add/remove files
         */
        addFiles.clear()
        addFiles.add(newMODEL001.toFile())
        addFiles.add(dat.toFile())
        removeFiles.clear()
        removeFiles.add(MODEL001)
        assert addFiles
        assert removeFiles
        /**
         * commit
         */
        ciMsg = "Make the second commit"
        gitManager.updateModel(git, addFiles, removeFiles, ciMsg)
        assertEquals(clone.listFiles().size(), 3) // .git, MODEL001.txt and simulation.dat
        println "After making the second commit:"
        clone.listFiles().each {
            println "Item: ${it.getName()}"
        }
        println "Finish!"
    }

    @Test
    void canRevertARevisionIfWeHaveAtLeastTwo() {
        def baseDir = new File("target/demoGitRevertRepo")
        baseDir.mkdirs()

        def git = Git.init()
            .setDirectory(baseDir)
            .call()
        try {
            def dotGit = new File(baseDir, ".git")
            assert dotGit.isDirectory()

            def importFile = new File(baseDir, "demo.txt")
            importFile.append("v1: ${new Date()}", "UTF-8")
            FileUtils.touch(importFile)
            GitSupport.importFile(git, importFile.name)
            GitSupport.makeCommit(git, "me", "me@example.com", "v1")

            importFile.append("v2: ${new Date()}", "UTF-8")
            FileUtils.touch importFile
            GitSupport.importFile(git, importFile.name)
            def v2 = GitSupport.makeCommit(git, "me", "me@example.com", "v2")
            def revisionToRevert = v2.name

            Repository repository = git.repository

            Ref headRef = repository.exactRef("HEAD")
            assertNotNull "HEAD reference is null, but it should not be", headRef

            def newHead = git.revert()
                .include(headRef)
                .call()

            assertNotNull "newHead is null", newHead
            assertNotEquals "revert commit not created", revisionToRevert, newHead.name

            def lines = Files.readAllLines(importFile.toPath(), StandardCharsets.UTF_8)
            assertEquals 1, lines.size()
            assertTrue lines.first().startsWith("v1")
        } finally {
            git.close()
        }

        assert baseDir.deleteDir(): "failed to delete $baseDir"
    }
}
