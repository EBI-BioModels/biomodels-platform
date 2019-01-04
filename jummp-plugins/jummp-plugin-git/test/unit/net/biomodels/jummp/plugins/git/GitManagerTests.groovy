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
* JGit, Apache Commons, Grails (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, Eclipse Distribution License v1.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of JGit, Apache Commons, Grails used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.plugins.git

import grails.test.GrailsUnitTestCase
import net.biomodels.jummp.core.vcs.*
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

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
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        repository = builder.setWorkTree(clone)
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir(clone) // scan up the file system tree
        .build()
        git = new Git(repository)
        git.init().setDirectory(clone).call()
    }

    protected void tearDown() {
        super.tearDown()
        FileUtils.deleteDirectory(new File("target/vcs/"))
    }

    void testInit() {
        shouldFail(VcsException) {
            // test that we cannot init into a file
            File noDirectory = new File("target/vcs/tmp")
            FileUtils.touch(noDirectory)
            gitManager.init(noDirectory, exchangeDirectory)
        }
        shouldFail(VcsException) {
            // test that we cannot init into a non-existing directory
            gitManager.init(new File("target/vcs/tmp2"), exchangeDirectory)
        }
        shouldFail(VcsException) {
            // test that exchange directory is not a file
            gitManager.init(clone, new File("target/vcs/tmp"))
        }
        shouldFail(VcsException) {
            // test that exchange directory exists
            gitManager.init(clone, new File("target/vcs/tmp3"))
        }
        // init should work now
        gitManager.init(clone, exchangeDirectory)
        shouldFail(VcsAlreadyInitedException) {
            // test that we cannot init the checkout twice
            gitManager.init(clone, exchangeDirectory)
        }
    }

        // The init method should work now
        gitManager.init(exchangeDirectory)
        shouldFail VcsAlreadyInitedException, {
            // test that we cannot initialise the checkout twice
            gitManager.init(exchangeDirectory)
        }
    }

    void testUpdate() {
        shouldFail(VcsNotInitedException) {
            gitManager.updateFile(new File("/tmp"), "tmp")
        }
        gitManager.init(clone, exchangeDirectory)
        shouldFail(VcsException) {
            // non existing file
            gitManager.updateFile(new File("target/vcs/tmp"), "test")
        }
        shouldFail(VcsException) {
            // directory instead of file
            File directory = new File("target/vcs/tmp")
            directory.mkdirs()
            gitManager.updateFile(directory, "test")
        }
        File importFile = new File("target/vcs/tmp/test")
        FileUtils.touch(importFile)
        shouldFail(FileNotVersionedException) {
            gitManager.updateFile(importFile, "test")
        }
        // import the file
        gitManager.importFile(importFile, "test")
        ObjectId commit = repository.resolve(Constants.HEAD)
        RevWalk revWalk = new RevWalk(repository)
        RevCommit revCommit = revWalk.parseCommit(commit)
        String prevCommit = revCommit.getName()
        // now update it
        importFile.append("Some test text\n")
        // verify commit message
        String revision = gitManager.updateFile(importFile, "test")
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        Repository repository = builder.setWorkTree(clone)
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir(clone) // scan up the file system tree
        .build()
        commit = repository.resolve(Constants.HEAD)
        revWalk = new RevWalk(repository)
        revCommit = revWalk.parseCommit(commit)
        assertTrue(prevCommit != commit.getName())
        assertEquals(commit.getName(), revision)
        assertEquals("Update of test", revCommit.getShortMessage())
        assertEquals("Update of test", revCommit.getFullMessage())
        // custom commit message
        importFile.append("Some more text")
        revision = gitManager.updateFile(importFile, "test", "Test commit message")
        repository = builder.setWorkTree(clone)
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir(clone) // scan up the file system tree
        .build()
        commit = repository.resolve(Constants.HEAD)
        revWalk = new RevWalk(repository)
        revCommit = revWalk.parseCommit(commit)
        assertEquals(commit.getName(), revision)
        assertEquals("Test commit message", revCommit.getShortMessage())
        assertEquals("Test commit message", revCommit.getFullMessage())
    }

    void testRetrieveFile() {
        shouldFail(VcsNotInitedException) {
            gitManager.retrieveFile("/tmp")
        }
        gitManager.init(clone, exchangeDirectory)
        shouldFail(FileNotVersionedException) {
            gitManager.retrieveFile("test")
        }
        File importFile = new File("target/vcs/tmp/test")
        FileUtils.touch(importFile)
        // import the file
        String importRevision = gitManager.importFile(importFile, "test")
        // now update it
        importFile.append("Some test text\n")
        String updateRevision = gitManager.updateFile(importFile, "test")
        // retrieve the file
        File retrievedFile = gitManager.retrieveFile("test")
        String path = retrievedFile.absolutePath
        assertTrue(retrievedFile.exists())
        assertTrue(retrievedFile.isFile())
        List<String> lines = retrievedFile.readLines()
        assertEquals(1, lines.size())
        assertEquals("Some test text", lines[0])
        // passing invalid revision should fail
        shouldFail(VcsException) {
            retrievedFile = gitManager.retrieveFile("test", "not a number")
        }
        shouldFailWithCause(MissingObjectException) {
            // some random sha1 sum should faild
            retrievedFile = gitManager.retrieveFile("test", "780f5fb1e98c9fb8fa96570bdafac121ddb0fd03")
        }
        retrievedFile = gitManager.retrieveFile("test", importRevision)
        assertTrue("Retrieved files have same path, but each retrieved file should have a unique identifier", path != retrievedFile.absolutePath)
        assertEquals(0, retrievedFile.readBytes().length)
        // clone has to be on HEAD again
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        Repository repository = builder.setWorkTree(clone)
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir(clone) // scan up the file system tree
        .build()
        ObjectId commit = repository.resolve(Constants.HEAD)
        RevWalk revWalk = new RevWalk(repository)
        RevCommit revCommit = revWalk.parseCommit(commit)
        assertEquals(updateRevision, revCommit.name())
    }

    void testUpdateWorkingCopy() {
        shouldFail(VcsNotInitedException) {
            gitManager.updateWorkingCopy()
        }
        gitManager.init(clone, exchangeDirectory)
        // TODO: write suited test case
    }
}
