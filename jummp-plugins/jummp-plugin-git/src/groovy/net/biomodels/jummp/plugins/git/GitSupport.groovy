/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with JGit, Apache Commons, Perf4j
 * (or a modified version of that library), containing parts covered by the terms of Apache License v2.0,
 * Eclipse Distribution License v1.0, the licensors of this Program grant you additional permission to
 * convey the resulting work. {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of JGit, Apache Commons, Perf4j used as well as
 * that of the covered work.}
 */

package net.biomodels.jummp.plugins.git

import org.apache.log4j.Logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.TreeWalk

import java.util.logging.Level

/**
 * This class provides tools and utils that support git manager, especially for testing purpose
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class GitSupport {
    static Repository buildRepository(File clone) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        Repository repository = builder.setWorkTree(clone)
            .readEnvironment() // scan environment GIT_* variables
            .findGitDir(clone) // scan up the file system tree
            .build()
        repository
    }

    /**
     * Import a given file in to the index of the specific repository
     *
     * @param git  A Git object
     * @param file  A file in question
     */
    static void importFile(Git git, String filePath) {
        git.add().
            addFilepattern(filePath).
            call()
    }

    /**
     * Do the commit with a message and an author
     * @param git   A Git object
     * @param ctMsg A string
     */
    static RevCommit makeCommit(Git git, String authorName, String authorEmail, String ctMsg) {
        git.commit().
            setAuthor(authorName, authorEmail).
            setMessage(ctMsg).call()
    }

    static getFullLatestCommitMessage(Repository repository) {
        RevCommit revCommit = parseCommit(repository)
        revCommit.getFullMessage()
    }

    static getLatestCommitHashString(Repository repository) {
        RevCommit revCommit = parseCommit(repository)
        revCommit.name
    }

    static List<String> lsFiles(Git git, RevCommit commit) {
        List<String> files = new ArrayList<String>()
        try {
            TreeWalk treeWalk = new TreeWalk(git.repository)
            treeWalk.addTree(new RevWalk(git.repository).parseTree(	commit))

            while (treeWalk.next()) {
                files.add(treeWalk.getPathString())
            }
        } finally {
            return files
        }
    }

    static List<String> lsFiles(Git git, String commitId) {
        Collection<String> result = new ArrayList<String>()
        try {
            // create a tree walk to search for files
            TreeWalk walk = new TreeWalk(git.getRepository())
            if (walk != null) {

                // recursively search fo files
                walk.setRecursive(true)
                // add the tree the specified commit belongs to
                walk.addTree(walk.getTree())

                // walk through the tree
                while (walk.next()) {

                    // TODO: is it a problem if mode is treemode?
                    final FileMode mode = walk.getFileMode(0)
                    if (mode == FileMode.TREE) {
                        println "GitSupport.lsFiles(): FileMode unexpected!"
                    }

                    // retrieve the path name of the current element
                    String fileName = walk.getPathString()

                    // we do not want to commit/checkout this file
                    result.add(fileName)

                }
            }

        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).
                log(Level.SEVERE, null, ex)
        }
        result
    }

    private static RevCommit parseCommit(Repository repository) {
        ObjectId commit = repository.resolve(Constants.HEAD)
        RevWalk revWalk = new RevWalk(repository)
        revWalk.parseCommit(commit)
    }
}
