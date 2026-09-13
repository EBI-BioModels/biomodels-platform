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
 * If you modify Jummp, or any covered work, by linking or combining it with
 * JGit, Apache Commons, Perf4j (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, Eclipse Distribution License v1.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 *{Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of JGit, Apache Commons, Perf4j used as well as
 * that of the covered work.}
 */


package net.biomodels.jummp.plugins.git

import net.biomodels.jummp.core.vcs.*
import org.eclipse.jgit.api.*
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.RefNotFoundException
import org.eclipse.jgit.errors.CheckoutConflictException
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.DepthWalk.RevWalk
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.perf4j.aop.Profiled
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
/**
 * @short GitManager provides the interface to a local git clone.
 *
 * This class is the concrete implementation of the VcsManager for git. It manages importing files,
 * updating files and retrieving files. Each model is stored as a separate directory with its own repository.
 * If a model repository does not exist, it is created. It uses the high level
 * API of JGit for managing the clone.
 *
 * GitManager uses ReentrantLocks in all of its method to make it thread safe
 * These locks are at the level of model repositories.
 * Nevertheless this does not ensure that the clone is kept in a correct state.
 * If two GitManager access the same model, the lock cannot protect and
 * GitManager is also not able to detect whether the model has been changed
 * outside the class. It is important to let the instance of the GitManager be
 * the only resource accessing the model repositories!
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class GitManager implements VcsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitManager.class)

    // uid for generating unique checkout directory names
    private static final AtomicInteger uid = new AtomicInteger(0)
    // locks to ensure model directories are not accessed concurrently
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<String, ReentrantLock>()
    private final ConcurrentHashMap<String, FileLock> diskLocks = new ConcurrentHashMap<String, FileLock>()
    // cache of initialised repositories
    private final Map<File, Git> initedRepositories = Collections.synchronizedMap(new LruCache<File, Git>(1000))
    // exchange directory
    private File exchangeDirectory
    // legacy parameter specifying remoteness. Probably useless.
    private boolean hasRemote

    /**
     * This internal class is a standard implementation of a cached hashmap
     * of fixed size, to avoid creating the repository related structures
     * repeatedly with the standard LRU caching policy.
     * @param < A >
     * @param < B >
     */
    class LruCache<A, B> extends LinkedHashMap<A, B> {
        private final int maxEntries

        LruCache(final int maxEntries) {
            super(maxEntries + 1, 1.0f, true)
            this.maxEntries = maxEntries
        }

        /**
         * Returns <tt>true</tt> if this <code>LruCache</code> has more entries than the maximum specified when it was
         * created.
         *
         * <p>
         * This method <em>does not</em> modify the underlying <code>Map</code> it relies on the implementation of
         * <code>LinkedHashMap</code> to do that, but that behavior is documented in the JavaDoc for
         * <code>LinkedHashMap</code>.
         * </p>
         *
         * @param eldest
         *            the <code>Entry</code> in question this implementation doesn't care what it is, since the
         *            implementation is only dependent on the size of the cache
         * @return <tt>true</tt> if the oldest
         * @see java.util.LinkedHashMap#removeEldestEntry(Map.Entry)
         */
        @Override
        protected boolean removeEldestEntry(final Map.Entry<A, B> eldest) {
            return super.size() > maxEntries
        }
    }

    /**
     * Initialises the GitManager. Sets the exchange directory for temporary
     * storage of model files
     */
    @Profiled(tag = "gitManager.init")
    void init(File exchangeDirectory) throws VcsException {
        if (this.exchangeDirectory == exchangeDirectory) {
            throw new VcsAlreadyInitedException()
        }
        if (exchangeDirectory.exists() && exchangeDirectory.isDirectory()) {
            this.exchangeDirectory = exchangeDirectory
        } else {
            throw new VcsException("${exchangeDirectory.name} is not a directory")
        }
    }

    @Profiled(tag = "gitManager.createModel")
    String createModel(File modelDirectory, List<File> modelFiles, String commit) {
        //FIXME this is meant to do Git-specific initialisation of the repository
        //then execute the same logic as importModel(modelDirectory, modelFiles, commit)
        return ""
    }

    private FileChannel getRepositoryChannel(File modelDirectory) {
        File repositoryFile = new File(modelDirectory, ".git/.locker.txt")
        FileChannel channel = new RandomAccessFile(repositoryFile, "rw").getChannel()
        return channel
    }

    private FileLock obtainExclusiveLock(File modelDirectory) throws VcsException {
        FileLock lock = null
        long accumulate = 0
        Exception lastException = null
        try {
            while (accumulate < 300000) {
                try {
                    FileChannel channel = getRepositoryChannel(modelDirectory)
                    lock = channel.tryLock()
                    //Write something to file, otherwise file isn't really locked
                    channel.write(ByteBuffer.wrap("\n".getBytes()))
                } catch (Exception ignore) {
                }
                if (lock) {
                    return lock
                }
                Thread.sleep(100)
                accumulate += 100
            }
            //lock=channel.lock()
        } catch (Exception e) {
            if (e instanceof InterruptedException && Thread.currentThread().isInterrupted()) {
                throw e // let upstream deal with it
            }
            lastException = e
        }
        if (!lock) {
            def ex = new VcsException("Error obtaining disk based lock, waited $accumulate ms")
            if (null != lastException) {
                ex.initCause(lastException)
            }
            throw ex
        }
        return lock
    }

    /**
     * Lock a model repository
     *
     * Checks whether a lock for the directory exists. If it does not exist
     * then it associates a lock with the model directory. It then acquires
     * the model directory lock
     * @param modelDirectory The directory to lock
     */
    @Profiled(tag = "gitManager.lockModelRepository")
    private void lockModelRepository(File modelDirectory) throws VcsException {
        if (!locks.containsKey(modelDirectory.name)) {
            ReentrantLock lock = new ReentrantLock()
            locks.put(modelDirectory.name, lock)
        }
        locks.get(modelDirectory.name).lock()
        FileLock fileLock = obtainExclusiveLock(modelDirectory)
        diskLocks.put(modelDirectory.name, fileLock)
    }

    /**
     * Unlock a model repository
     *
     * Unlocks the model directory, and if no other threads are waiting on it
     * the lock is removed from the locks container
     * @param modelDirectory The directory to unlock
     */
    @Profiled(tag = "gitManager.unlockModelRepository")
    private void unlockModelRepository(File modelDirectory) {
        ReentrantLock lock = locks.get(modelDirectory.name)
        if (!lock.hasQueuedThreads()) locks.remove(modelDirectory)
        FileLock removing = diskLocks.remove(modelDirectory.name)
        new File(modelDirectory, ".git/.locker.txt").setText("")
        removing.release()
        removing.channel().close()
        lock.unlock()
    }

    private void ensureRepInited(File modelDirectory) {
        if (!initedRepositories.containsKey(modelDirectory)) {
            if (exchangeDirectory == null) {
                throw new VcsException("init error: exchange directory cannot be null")
            }
            initRepository(modelDirectory)
        }
    }

    /**
     * Initialise a model directory
     *
     * Creates a repository in the model directory if the directory is valid
     * and the repository does not already exist. Caches the repository data
     * structures.
     * @param modelDirectory The model directory
     */
    @Profiled(tag = "gitManager.initRepository")
    private void initRepository(File modelDirectory) {
        if (initedRepositories.containsKey(modelDirectory)) {
            throw new VcsAlreadyInitedException()
        }
        if (exchangeDirectory == null) {
            throw new VcsException("Exchange directory cannot be null!")
        }
        if (!modelDirectory.isDirectory() || !modelDirectory.exists()) {
            throw new InvalidVcsRepositoryException(modelDirectory.toString())
        }
        if (!exchangeDirectory.isDirectory() || !exchangeDirectory.exists()) {
            throw new VcsException("""Exchange directory ${
                exchangeDirectory.getCanonicalPath()
            } is either not a directory ${exchangeDirectory.isDirectory()} or does not exist  ${
                !exchangeDirectory.exists()
            }""")
        }
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        Repository repository = builder.setWorkTree(modelDirectory)
            .readEnvironment() // scan environment GIT_* variables
            .setGitDir(modelDirectory) // use the current directory for the repository
            .build()
        Git git = new Git(repository)

        String branchName
        String fullBranch = repository.getFullBranch()

        // create the repository if it doesn't exist
        if (!fullBranch) {
            git = createGitRepo(modelDirectory)
            repository = git.getRepository()
            fullBranch = repository.getFullBranch()

        }
        branchName = fullBranch.substring(Constants.R_HEADS.length())
        Config repoConfig = repository.getConfig()

        // this bit is probably unnecessary
        final String remote = repoConfig.getString(
            ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
            ConfigConstants.CONFIG_KEY_REMOTE)
        hasRemote = (remote != null)
        initedRepositories.put(modelDirectory, git)
    }

    /**
     * Equivalent to running git init on the directory supplied.
     * @param directory The model directory
     * @return Git object
     */
    @Profiled(tag = "gitManager.createGitRepo")
    private Git createGitRepo(File directory) {
        Git git = null
        InitCommand initCommand = Git.init()
        initCommand.setDirectory(directory)
        git = initCommand.call()
        git
    }

    /**
     * Updates a model with the supplied files and default commit message (e.g. Update of Model ABC)
     *
     * Overload of updateModel with a default message
     * @param modelDirectory The model directory
     * @param addFiles A list of the supplied files to be put into the repository
     */
    @Profiled(tag = "gitManager.updateModel")
    String updateModel(File modelDirectory, List<File> addFiles,
                       List<File> removeFiles, boolean isAmend = false)
            throws VcsException {
        String cmtMsg = "Update of ${modelDirectory.name}"
        return updateModel(modelDirectory, addFiles, removeFiles, cmtMsg, isAmend)
    }

    /**
     * Updates a model with the supplied files and commit message
     * Locks model, initialises the repository if necessary and adds
     * the supplied files to the repository with the supplied message
     * @param modelDirectory The model directory
     * @param files A list of files to be put into the repository
     * @param removeFiles The list of files to be removed from the repository.
     * @param commitMessage The commit message for this revision
     */
    @Profiled(tag = "gitManager.updateModel")
    String updateModel(File modelDirectory, List<File> addFiles,
                       List<File> removeFiles, String commitMessage,
                       boolean isAmend = false) throws VcsException {
        ensureRepInited(modelDirectory)
        String revision = null
        lockModelRepository(modelDirectory)
        try {
            revision = handleModification(modelDirectory, addFiles, removeFiles, commitMessage, isAmend)
        } finally {
            unlockModelRepository(modelDirectory)
        }
        revision
    }

    List<VcsFileDetails> getFileDetails(File modelDirectory, String path) {
        List<VcsFileDetails> fileDetails = new ArrayList<VcsFileDetails>()
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder()
            Repository repository
            repository = builder
                .setGitDir(new File(".git", modelDirectory.name))
                .readEnvironment()
                .findGitDir().build()

            Git git = new Git(repository)
            RevWalk walk = new RevWalk(repository, 100)
            RevCommit commit = null
            LogCommand cmd = git.log()
            cmd.addPath(path)
            Iterable<RevCommit> logs = cmd.call()
            Iterator<RevCommit> i = logs.iterator()

            while (i.hasNext()) {
                def iterated = i.next()
                commit = walk.parseCommit(iterated)
                long timestamp = commit.getCommitTime()
                VcsFileDetails detail = new VcsFileDetails()
                detail.revisionId = iterated.getName()
                detail.commit = timestamp * 1000
                detail.msg = commit.getFullMessage()
                fileDetails.add(detail)
            }
        } catch (Exception ex) {
            throw new IOException("Git command could not be executed", ex)
        }
        return fileDetails
    }

    void resetModelRepository(File modelDirectory, String commitId) throws VcsException {
        Repository repository = GitSupport.buildRepository(modelDirectory)
        Git git = new Git(repository)
        git.init().setDirectory(modelDirectory).call()
        try {
            ResetCommand resetCmd = git.reset()
            resetCmd.setRef(commitId)
            resetCmd.setMode(ResetCommand.ResetType.HARD)
            resetCmd.call()
        } catch (GitAPIException | CheckoutConflictException ex) {
            String errMsg = "Exception thrown during git reset $commitId in ${modelDirectory.name}"
            throw new VcsException(errMsg, ex)
        } finally {
            repository.close()
        }
    }

    /**
     * Removes a single commit from a model's history and replays every later commit
     * on top of its parent.
     *
     * There is no git primitive to delete a commit in place, so this does a cherry-pick
     * replay: it walks the log (assumed linear - this class never creates merge commits),
     * finds @p commitId, checks out a scratch branch at its parent, cherry-picks every
     * commit after it in original order, then force-moves the real branch to the
     * rebuilt tip and discards the scratch branch. Every replayed commit gets a new id;
     * the old-to-new mapping is returned so the caller can update any persisted
     * revision id (e.g. Revision.vcsId) that pointed at the old ids.
     *
     * Refuses to delete the first commit of the repository, since there is no parent
     * to reparent onto - callers should delete the whole Model in that case.
     *
     * @param modelDirectory The model directory
     * @param commitId The commit to remove, identified by its SHA-1
     * @return A Map from each replayed commit's original id to its new id, ordered
     *         oldest first
     */
    @Profiled(tag = "gitManager.deleteCommit")
    Map<String, String> deleteCommit(File modelDirectory, String commitId) throws VcsException {
        ensureRepInited(modelDirectory)
        lockModelRepository(modelDirectory)
        Map<String, String> shaMapping = new LinkedHashMap<String, String>()
        String tempBranch = "delete-${commitId}-${UUID.randomUUID()}"
        Git git = initedRepositories.get(modelDirectory)
        String originalBranch = git.repository.branch
        boolean onTempBranch = false
        try {
            List<RevCommit> ordered = git.log().call().toList().reverse() // oldest first
            int targetIndex = ordered.findIndexOf { it.name == commitId }
            if (targetIndex < 0) {
                throw new VcsException("Commit $commitId not found in ${modelDirectory.name}")
            }
            if (targetIndex == 0) {
                throw new VcsException("""Cannot delete the first commit of \
${modelDirectory.name} via deleteCommit; delete the Model instead""")
            }
            List<RevCommit> toReplay = ordered.subList(targetIndex + 1, ordered.size())
            String parentSha = ordered[targetIndex - 1].name

            if (!git.status().call().clean) {
                git.stashCreate().call()
            }

            git.checkout().setCreateBranch(true).setName(tempBranch).setStartPoint(parentSha).call()
            onTempBranch = true

            toReplay.each { RevCommit original ->
                CherryPickResult result = git.cherryPick().include(original).call()
                if (result.status != CherryPickResult.CherryPickStatus.OK) {
                    throw new VcsException("""Cherry-pick of ${original.name} failed with \
status ${result.status} while deleting $commitId in ${modelDirectory.name}""")
                }
                shaMapping.put(original.name, result.newHead.name)
            }

            git.branchCreate().setName(originalBranch).setStartPoint(tempBranch).setForce(true).call()
            git.checkout().setName(originalBranch).call()
            onTempBranch = false
            git.branchDelete().setBranchNames(tempBranch).setForce(true).call()
        } catch (GitAPIException gitEx) {
            throw new VcsException(
                "Exception thrown while deleting commit $commitId in ${modelDirectory.name}", gitEx)
        } finally {
            try {
                if (onTempBranch) {
                    // leave no half-finished cherry-pick behind; original history is untouched
                    // since we never force-moved originalBranch in this branch of execution
                    git.checkout().setForced(true).setName(originalBranch).call()
                }
                if (git.branchList().call().find { it.name == "refs/heads/$tempBranch" }) {
                    git.branchDelete().setBranchNames(tempBranch).setForce(true).call()
                }
            } catch (Exception cleanupEx) {
                LOGGER.warn("Could not clean up scratch branch $tempBranch in ${modelDirectory.name}", cleanupEx)
            }
            unlockModelRepository(modelDirectory)
        }
        return shaMapping
    }

    /**
     * Convenience function for copying files from a given directory
     * to exchange, and passing the file objects back
     *
     * @param modelDirectory The model directory where files are to be copied from
     * @param addHere a list object where the created file objects are stored
     */
    @Profiled(tag = "gitManager.downloadFiles")
    private void downloadFiles(File modelDirectory, List<File> addHere) {
        File[] repFiles = modelDirectory.listFiles()
        String path = exchangeDirectory.absolutePath + File.separator + UUID.randomUUID().toString()
        File tempDir = new File(path)
        tempDir.mkdir()
        repFiles.each {
            String filePath = tempDir.absolutePath + File.separator + it.getName()
            File targetFile = new File(filePath)
            if (!it.isDirectory()) {
                Path sourcePath = it.toPath()
                Path targetPath = Paths.get(tempDir.absolutePath, it.getName())
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                addHere.add(targetFile)
            }
        }
        if (addHere.isEmpty()) throw new VcsException("Model directory is empty!")
    }

    /**
     * Retrieves files associated with the latest revision of a model
     *
     * Same as calling retrieveModel(modelDirectory, null)
     * @param   modelDirectory The model directory
     * @return  a list of the files associated with the the latest revision
     */
    @Profiled(tag = "gitManager.retrieveModel")
    List<File> retrieveModel(File modelDirectory) throws VcsException {
        return retrieveModel(modelDirectory, null)
    }

    /**
     * Retrieves files associated with the specified revision of a model
     *
     * Locks model directory. If the current revision is requested (by specifying
     * null as the revision) the files currently in the model directory are
     * copied into the exchange directory. If an earlier revision is requested
     * the repository is first set to the requested revision, the files are downloaded
     * to exchange directory, before the repository is set back to the latest revision.
     * @param modelDirectory The model directory
     * @param revision the string indicates the starting point of the checkout. It could be a hash string, branch name, tag name, etc. In the context of JUMMP, this is the vcs identifier of a specific revision.
     * If this is null, HEAD will be used.
     * @return a list of the files associated with the given revision
     */
    @Profiled(tag = "gitManager.retrieveModel")
    List<File> retrieveModel(File modelDirectory, String revision) throws VcsException {
        ensureRepInited(modelDirectory)
        List<File> returnedFiles = new LinkedList<File>()
        lockModelRepository(modelDirectory)
        try {
            if (revision == null) {
                // return current HEAD revision
                downloadFiles(modelDirectory, returnedFiles)
            } else {
                if (!getRevisionsPrivate(modelDirectory, false).containsKey(revision))
                    throw new VcsException("Revision '$revision' not found in model directory '$modelDirectory' !")
                String branchName = ""
                try {
                    // need to checkout in a temporary branch
                    branchName = UUID.randomUUID()
                    if (!initedRepositories.get(modelDirectory).status().call().clean) {
                        LOGGER.debug("Revision: $revision, model directory: ${modelDirectory.name}, is unclean.")
                        initedRepositories.get(modelDirectory).stashCreate().call()
                    }
                    initedRepositories.get(modelDirectory).
                        checkout().
                        setCreateBranch(true).
                        setName(branchName).
                        setStartPoint(revision).
                        call()
                    downloadFiles(modelDirectory, returnedFiles)
                } catch (VcsException e) {
                    throw new VcsException("Checking out file from git directory ${modelDirectory?.name} failed: ", e)
                } catch (RefNotFoundException rnfEx) {
                    throw new VcsException("""Error while checking out the revision $revision of \
the model git directory ${modelDirectory?.absolutePath}""", rnfEx)
                } finally {
                    if (modelDirectory && branchName) {
                        // clear all the recently changes made and checkout the master brain
                        Git git = initedRepositories.get(modelDirectory)
                        doGitCleanAndCheckoutMasterBranch(git, branchName)
                    }
                }
            }
        } catch (VcsException e) {
            String errMsg = """The working directory of the revision ${revision} at ${modelDirectory.absolutePath} \
has not been initialised any VCS yet."""
            throw new VcsException(errMsg, e)
        } finally {
            unlockModelRepository(modelDirectory)
        }
        return returnedFiles
    }

    /**
     * Retrieves the revisions associated with the model by looking at the git log.
     *
     * Locks model directory. Iterates through the git log, adding the revision
     * id associated with each commit to the returned map.
     * @param modelDirectory The model directory
     */
    @Profiled(tag = "gitManager.getRevisions")
    Map getRevisions(File modelDirectory) throws VcsException {
        return getRevisionsPrivate(modelDirectory, true)
    }

    /**
     * Retrieves the revisions associated with the model by looking at the git log with optional locking.
     *
     * Convenience function with flag for specifying whether or not to lock model directory.
     * As FileLocks are not re-entrant, when the function is called from within the class
     * where the lock has already been acquired, set the flag false.
     * @param modelDirectory The model directory
     * @param acquireLocks Whether or not to acquire locks.
     */
    @Profiled(tag = "gitManager.getRevisionsPrivate")
    private Map getRevisionsPrivate(File modelDirectory, boolean acquireLocks) {
        ensureRepInited(modelDirectory)
        Map mapRev = new HashMap()
        if (acquireLocks) {
            lockModelRepository(modelDirectory)
        }
        try {
            Iterator<RevCommit> log = initedRepositories.get(modelDirectory).log().call().iterator()
            log.each {
                PersonIdent authorIdent = it.getAuthorIdent()
                Date authorDate = authorIdent.getWhen()
                // TimeZone authorTimeZone = authorIdent.getTimeZone()
                // PersonIdent committerIdent = it.getCommitterIdent()
                LOGGER.debug "${it.name}: ${authorDate}: ${it.shortMessage}"
                Map map = [date: authorDate, message: it.fullMessage]
                mapRev.put(it.name, map)
            }
        } finally {
            if (acquireLocks) {
                unlockModelRepository(modelDirectory)
            }
        }
        return mapRev
    }

    /**
     * Multi-file per model version of legacy remote repository implementation
     * This is currently untested. The logic is the same as the single repository
     * implementation, however it is currently only acting on the cached repositories.
     * DO NOT USE AS IS FOR REMOTE REPOSITORIES. Untested mapping of legacy code
     * to new data structures, mainly for the purposes of keeping interfaces.
     * current and compiling.
     * @param modelDirectory The model directory
     */
    @Profiled(tag = "gitManager.updateWorkingCopy")
    void updateWorkingCopy(File modelDirectory) throws VcsException {
        ensureRepInited(modelDirectory)
        lockModelRepository(modelDirectory)
        try {
            if (hasRemote) {
                initedRepositories.get(modelDirectory).pull().call()
            }
        } finally {
            unlockModelRepository(modelDirectory)
        }
    }


    /**
     * This is used for testing the private method handleModification.
     * handleModification is a core manipulation of GitManager where
     * all add and rm git operations should get tackled. To ease testing,
     * this method is doing to create a public service accepting a git instance
     * without looking for a manually initialised git object from initedRepositories
     * that are dependent on the model repositories.
     *
     * @param git   A Git object
     * @param files A list of files to be added
     * @param deleted A list of files to be removed
     * @param commitMessage A string representing the commit message
     *
     * @return A String denoting the commit id of the newly made commit
     */
    String updateModel(Git git, boolean isAmend = false, List<File> files, List<File> deleted, String commitMessage) {
        String revision
        try {
            revision = doGitUpdate(git, isAmend, files, deleted, commitMessage)
        } catch (Exception e) {
            e.printStackTrace()
            throw new IOException("Git command could not be executed", e)
        }
        return revision
    }

    /**
     * Internal implementation for git add/git commit.
     *
     * In git there is no difference between initial import and update of a file.
     * This method contains the merged implementation for both import and update.
     * It locks the model directory, initialises if necessary
     * copies the files, does git add, git commit and finally a push
     *
     * @param modelDirectory The model directory
     * @param files The files to copy into the directory
     * @param deleted The files that will be deleted
     * @param commitMessage The commit message
     * @return A String A string representing the commit hash of the recently created revision
     */
    @Profiled(tag = "gitManager.handleModification")
    private String handleModification(File modelDirectory, List<File> files,
                                      List<File> deleted,
                                      String commitMessage, boolean isAmend = false) {
        String revision
        try {
            Git git = initedRepositories.get(modelDirectory)
            revision = doGitUpdate(git, isAmend, files, deleted, commitMessage)
        } catch (Exception e) {
            e.printStackTrace()
            throw new IOException("Git command could not be executed", e)
        }
        return revision
    }

    /**
     * This method tries to remove the files which need to be remove and add the ones which
     * are going to with the latest commit. Flipping around remove and add operation aims at
     * preserving the files in the list of removed or added files which names are the same.
     *
     *
     * @param git       A git object holding the model repository
     * @param files     A list of the files to be added
     * @param deleted   A list of the files to be removed
     * @param commitMessage A string denoting the commit message
     *
     * @return A String denoting the commit id of the newly made commit
     */
    private String doGitUpdate(Git git, boolean isAmend = false,
                               List<File> files, List<File> deleted, String commitMessage) {
        String revision
        String repoDir = git.repository.directory.parent // parent of .git dir
        File modelDirectory = new File(repoDir)
        if (deleted) {
            RmCommand rm = git.rm()
            deleted.each {
                rm = rm.addFilepattern(it.getName())
            }
            rm.call()
        }
        if (files) {
            AddCommand add = git.add()
            files.each {
                Path sourcePath = it.toPath()
                Path targetPath = Paths.get(modelDirectory.absolutePath, it.getName())
                Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                add = add.addFilepattern(it.getName())
            }
            add.call()
        }
        RevCommit commit = git
                            .commit()
                            .setAmend(isAmend)
                            .setNoVerify(true)
                            .setMessage(commitMessage)
                            .call()
        revision = commit.getId().getName()
        /*if (hasRemote) {
              git.push().call()
        }*/
        revision
    }

    /**
     * Cleans or stashes the temporary branch if there has been changes.
     * Then do checkout the master or main branch.
     *
     * @param git A Git object representing the model git directory in question
     * @param branchName A String representing the provisional branch
     */
    private void doGitCleanAndCheckoutMasterBranch(Git git, String branchName) {
        try {
            LOGGER.debug("Checking out the master branch...")
            if (!git.status().call().clean) {
                git.stashCreate().call()
            }
            Ref ref = git.branchList().call().find { it.name == "refs/heads/master" }
            if (ref) {
                git.checkout().setName("master").call()
            } else {
                ref = git.branchList().call().find { it.name == "refs/heads/main" }
                if (ref) { git.checkout().setName("main").call() }
            }
        } catch (RefNotFoundException rnfException) {
            String gitDir = git?.getRepository()?.directory?.name
            String msg = "Cannot find the branch $branchName under the Git repository ${gitDir}"
            LOGGER.error(msg, rnfException)
            // the println statement aims to send the message to the stdout for ELK
            println("$msg\n${rnfException.toString()}")
        } finally {
            git.branchDelete().setBranchNames(branchName).call()
        }
    }
}
