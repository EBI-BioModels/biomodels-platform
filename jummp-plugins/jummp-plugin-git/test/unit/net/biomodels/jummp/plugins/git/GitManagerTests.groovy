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
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.junit.Test

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.junit.Assert.*

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
        File clone = new File("target/vcs/clone")
        assertNull new File(".git", clone).canonicalPath
    }


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
        assertEquals(clone.listFiles().size(), 2) // .git and simulation.dat
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
