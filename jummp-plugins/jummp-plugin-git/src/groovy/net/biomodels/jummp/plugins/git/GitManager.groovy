/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of JGit, Apache Commons, Perf4j used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.plugins.git

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import net.biomodels.jummp.core.vcs.VcsManager
import net.biomodels.jummp.core.vcs.VcsException
import net.biomodels.jummp.core.vcs.VcsFileDetails
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.AddCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.InitCommand
import org.eclipse.jgit.api.LogCommand
import org.eclipse.jgit.api.RmCommand
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.DepthWalk.RevWalk
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.perf4j.aop.Profiled

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
 */
class GitManager implements VcsManager {
    // uid for generating unique checkout directory names
    private static final AtomicInteger uid = new AtomicInteger(0)
    // locks to ensure model directories are not accessed concurrently
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<String, ReentrantLock>()
    private final ConcurrentHashMap<String, FileLock> diskLocks = new ConcurrentHashMap<String, FileLock>()
    // cache of initialised repositories
    private final Map<File, Git>  initedRepositories = Collections.synchronizedMap(new LruCache<File, Git>(20))
    // exchange directory
    private File exchangeDirectory
    // legacy parameter specifying remoteness. Probably useless.
    private boolean hasRemote

    /* 
     * This internal class is a standard implementation of a cached hashmap
     * of fixed size, to avoid creating the repository related structures
     * repeatedly with the standard LRU caching policy
     * */
    class LruCache<A, B> extends LinkedHashMap<A, B> {
        private final int maxEntries

        public LruCache(final int maxEntries) {
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
     * Initialises the GitManager. Sets the exchangedirectory for temporary
     * storage of model files
     **/
    @Profiled(tag = "gitManager.init")
    public void init(File exchangeDirectory) {
        this.exchangeDirectory = exchangeDirectory
    }

    @Profiled(tag = "gitManager.createModel")
    public String createModel(File modelDirectory, List<File> modelFiles, String commit) {
        //FIXME this is meant to do Git-specific initialisation of the repository
        //then execute the same logic as importModel(modelDirectory, modelFiles, commit)
        return ""
    }

    private FileChannel getRepositoryChannel(File modelDirectory) {
        File repositoryFile = new File(modelDirectory, ".git/.locker.txt")
        FileChannel channel = new RandomAccessFile(repositoryFile, "rw").getChannel()
        return channel
    }

    private FileLock obtainExclusiveLock(File modelDirectory) {
        FileLock lock = null
        long accumulate = 0
        try {
            while (accumulate < 300000) {
                try {
                    FileChannel channel = getRepositoryChannel(modelDirectory)
                    lock = channel.tryLock()
                    //Write something to file, otherwise file isnt really locked
                    channel.write(ByteBuffer.wrap("\n".getBytes()))
                } catch(Exception ignore) {
                }
                if (lock) {
                    return lock
                }
                Thread.sleep(100)
                accumulate += 100
            }
            //lock=channel.lock()
        } catch(Exception e) {
            e.printStackTrace()
        }
        if (!lock) {
            throw new Exception("Error obtaining disk based lock, waited $accumulate ms")
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
    private void lockModelRepository(File modelDirectory) {
        if (!locks.containsKey(modelDirectory.name))
        {
            ReentrantLock lock = new ReentrantLock()
            locks.put(modelDirectory.name, lock)
        }
        locks.get(modelDirectory.name).lock()
        FileLock fileLock=obtainExclusiveLock(modelDirectory)
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
        ReentrantLock lock=locks.get(modelDirectory.name)
        if (!lock.hasQueuedThreads()) locks.remove(modelDirectory)
        FileLock removing=diskLocks.remove(modelDirectory.name)
        new File(modelDirectory, ".git/.locker.txt").setText("")
        removing.release()
        removing.channel().close()
        lock.unlock()
    }

    private void ensureRepInited(File modelDirectory) {
        if (!initedRepositories.containsKey(modelDirectory)) {
            if (exchangeDirectory==null) throw new VcsException("init error: exchange directory cannot be null")
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
            //throw new VcsAlreadyInitedException()
            return
        }
        if (exchangeDirectory == null) {
            throw new VcsException("Exchange directory cannot be null!")
        }
        if (!modelDirectory.isDirectory() || !modelDirectory.exists()) {
            throw new VcsException("Local model directory " + modelDirectory.toString() + " is either not a directory or does not exist")
        }
        if (!exchangeDirectory.isDirectory() || !exchangeDirectory.exists()) {
            throw new VcsException("Exchange directory " + exchangeDirectory.getCanonicalPath() + " is either not a directory ${exchangeDirectory.isDirectory()} or does not exist ${!exchangeDirectory.exists()}")
        }
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        Repository repository = builder.setWorkTree(modelDirectory)
                .readEnvironment() // scan environment GIT_* variables
                .setGitDir(modelDirectory) // use the current directory for the repository
                .build()
        Git git = new Git(repository)

        String branchName
        String fullBranch = repository.getFullBranch()

        //create the repository if it doesnt exist
        if (!fullBranch) {
            git=createGitRepo(modelDirectory)
            repository=git.getRepository()
            fullBranch=repository.getFullBranch()

        }
        branchName = fullBranch.substring(Constants.R_HEADS.length())
        Config repoConfig = repository.getConfig()

        //this bit is probably unnecessary
        final String remote = repoConfig.getString(
            ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
            ConfigConstants.CONFIG_KEY_REMOTE)
        hasRemote = (remote != null)
        initedRepositories.put(modelDirectory,git)
    }

    /*
     * Equivalent to running git init on the directory supplied.
     * @param modelDirectory The model directory
     **/
    @Profiled(tag = "gitManager.createGitRepo")
    private Git createGitRepo(File directory) {
        Git git=null
        InitCommand initCommand = Git.init()
        initCommand.setDirectory(directory)
        git=initCommand.call()
        return git
    }

    /*
     * Updates a model with the suppled files and commit message
     * 
     * Locks model, initialises the repository if necessary and adds
     * the supplied files to the repository with the supplied message
     * @param modelDirectory The model directory
     * @param files A list of files to be put into the repository
     * @param removeFiles The list of files to be removed from the repository.
     * @param commitMessage The commit message for this revision
     **/
    @Profiled(tag = "gitManager.updateModel")
    public String updateModel(File modelDirectory, List<File> addfiles, List<File> removeFiles, String commitMessage) {
        ensureRepInited(modelDirectory)
        String revision = null
        lockModelRepository(modelDirectory)
        try {
            revision = handleModification(modelDirectory, addfiles, removeFiles, commitMessage)
        } finally {
            unlockModelRepository(modelDirectory)
        }
        return revision
    }
    
    
    public List<VcsFileDetails> getFileDetails(File modelDirectory, String path) {
    	List<VcsFileDetails> fileDetails = new ArrayList<VcsFileDetails>()
    	try {
    		FileRepositoryBuilder builder = new FileRepositoryBuilder()
    		Repository repository;
            repository = builder.setGitDir(new File(".git", modelDirectory)).readEnvironment()
                                                  .findGitDir().build();

            Git git = new Git(repository)
            RevWalk walk = new RevWalk(repository,100);
            RevCommit commit = null;
			LogCommand cmd = git.log();
			cmd.addPath(path);
			Iterable<RevCommit> logs = cmd.call();
			Iterator<RevCommit> i = logs.iterator();
			
			while (i.hasNext()) {
				def iterated = i.next()
				commit = walk.parseCommit( iterated );
				long timestamp = commit.getCommitTime();
				VcsFileDetails detail = new VcsFileDetails();
				detail.revisionId=iterated.getName()
				detail.commit=timestamp * 1000
				detail.msg=commit.getFullMessage()
				fileDetails.add(detail);
			}   
		} 
		catch (Exception ex) {
				throw new IOException("Git command could not be executed", ex)	
		}
		return fileDetails
    }

    /*
     * Updates a model with the suppled files and default commit message
     * 
     * Overload of updateModel with a default message
     * @param modelDirectory The model directory
     * @param files A list of files to be put into the repository
     **/
    @Profiled(tag = "gitManager.updateModel")
    public String updateModel(File modelDirectory, List<File> addfiles, List<File> removeFiles) {
        return updateModel(modelDirectory, addfiles, removeFiles, "Update of ${modelDirectory.name}")
    }

    /*
     * Convenience function for copying files from a given directory
     * to exchange, and passing the file objects back
     * 
     * @param modelDirectory The model directory where files are to be copied from
     * @param addHere a list object where the created file objects are stored
     **/
    @Profiled(tag = "gitManager.downloadFiles")
    private void downloadFiles(File modelDirectory, List<File> addHere)
    {
        File[] repFiles=modelDirectory.listFiles()
        File tempDir = new File (exchangeDirectory.absolutePath + System.getProperty("file.separator") + UUID.randomUUID().toString() )
        tempDir.mkdir()
        repFiles.each
        {
            File destinationFile = new File(tempDir.absolutePath + System.getProperty("file.separator") + it.getName())
            if (!it.isDirectory())
            {
                FileUtils.copyFile(it, destinationFile)
                addHere.add(destinationFile)
            }
        }
        if (addHere.isEmpty()) throw new VcsException("Model directory is empty!")

    }

    /*
     * Retrieves files associated with the latest revision of a model
     * 
     * Same as calling retrieveModel(modelDirectory, null)
     * @param modelDirectory The model directory
     **/
    @Profiled(tag = "gitManager.retrieveModel")
    public List<File> retrieveModel(File modelDirectory) {
        return retrieveModel(modelDirectory, null)
    }

    /*
     * Retrieves files associated with the specified revision of a model
     * 
     * Locks model directory. If the current revision is requested (by specifying
     * null as the revision) the files currently in the model directory are
     * copied into the exchange directory. If an earlier revision is requested
     * the repository is first set to the requested revision, the files are downloaded
     * to exchange, before the repository is set back to the latest revision.
     * @param modelDirectory The model directory
     * @param revision The revision of the model requested
     **/
    @Profiled(tag = "gitManager.retrieveModel")
    public List<File> retrieveModel(File modelDirectory, String revision) {
        ensureRepInited(modelDirectory)
        List<File> returnedFiles = new LinkedList<File>()
        lockModelRepository(modelDirectory)
        try {
            if (revision == null) {
                // return current HEAD revision
                downloadFiles(modelDirectory, returnedFiles)
            } else {
                if (!getRevisionsPrivate(modelDirectory,false).contains(revision))
                throw new VcsException("Revision '$revision' not found in model directory '$modelDirectory' !")
                try {
                    // need to checkout in a temporary branch
                    String branchName = UUID.randomUUID()
                    initedRepositories.get(modelDirectory).
                                       checkout().
                                       setName(branchName).
                                       setCreateBranch(true).
                                       setStartPoint(revision).
                                       call()
                    downloadFiles(modelDirectory, returnedFiles)
                    initedRepositories.get(modelDirectory).checkout().setName("master").call()
                    initedRepositories.get(modelDirectory).branchDelete().setBranchNames(branchName).call()
                } catch (Exception e) {
                    throw new VcsException("Checking out file from git failed: ", e)
                }
            }
        } finally {
            unlockModelRepository(modelDirectory)
        }
        return returnedFiles
    }

    /*
     * Retrieves the revisions associated with the model by looking at the git log
     * 
     * Locks model directory. Iterates through the git log, adding the revision
     * id associated with each commit to the returned list.
     * @param modelDirectory The model directory
     **/
    @Profiled(tag = "gitManager.getRevisions")
    public List<String> getRevisions(File modelDirectory) {
        return getRevisionsPrivate(modelDirectory, true)
    }

    /*
     * Retrieves the revisions associated with the model by looking at the git log with optional locking
     * 
     * Convenience function with flag for specifying whether or not to lock model directory. 
     * As FileLocks are not re-entrant, when the function is called from within the class
     * where the lock has already been acquired, set the flag false. 
     * @param modelDirectory The model directory
     * @param acquireLocks Whether or not to acquire locks.
     **/
    @Profiled(tag = "gitManager.getRevisionsPrivate")
    private List<String> getRevisionsPrivate(File modelDirectory, boolean acquireLocks) {
        ensureRepInited(modelDirectory)
        List<String> myList=new LinkedList<String>()
        if (acquireLocks) {
            lockModelRepository(modelDirectory)
        }
        try {
            Iterator<RevCommit> log=initedRepositories.get(modelDirectory).log().call().iterator()
            log.each
            {
                myList.add(it.getName())
            }
        }
        finally {
            if (acquireLocks) {
                unlockModelRepository(modelDirectory)
            }
        }
        return myList
    }

    /*
     * Multi-file per model version of legacy remote repository implementation
     * 
     * This is currently untested. The logic is the same as the single repository
     * implementation, however it is currently only acting on the cached repositories.
     * DO NOT USE AS IS FOR REMOTE REPOSITORIES. Untested mapping of legacy code
     * to new data structures, mainly for the purposes of keeping interfaces 
     * current and compiling.
     * @param modelDirectory The model directory
     **/
    @Profiled(tag = "gitManager.updateWorkingCopy")
    public void updateWorkingCopy(File modelDirectory) {
        ensureRepInited(modelDirectory)
        lockModelRepository(modelDirectory)
        try {
            if (hasRemote) {
                initedRepositories.get(modelDirectory).pull().call()
            }
        }
        finally {
            unlockModelRepository(modelDirectory)
        }
    }

    /**
     * Internal implementation for git add/git commit.
     *
     * In git there is no difference between initial import and update of a file.
     * This method contains the merged implementation for both import and update.
     * It locks the model directory, initialises if necessary
     * copies the files, does git add, git commit and finally a push
     * @param modelDirectory The model directory
     * @param files The files to copy into the directory
     * @param deleted The files that will be deleted
     * @param commitMessage The commit message 
     */
    @Profiled(tag = "gitManager.handleModification")
    private String handleModification(File modelDirectory, List<File> files, List<File> deleted, String commitMessage) {
        String revision
        try {
            //updateWorkingCopy(modelDirectory)
            Git git = initedRepositories.get(modelDirectory)
            if (files) {
                AddCommand add = git.add()
                files.each {
                    FileUtils.copyFile(it, new File(modelDirectory.absolutePath + File.separator + it.getName()))
                    add = add.addFilepattern(it.getName())
                }
                add.call()
            }
            if (deleted) {
                RmCommand rm = git.rm()
                deleted.each {
                    rm = rm.addFilepattern(it.getName())
                }
                rm.call()
            }
            RevCommit commit = git.commit().setMessage(commitMessage).call()
            revision = commit.getId().getName()
            /*if (hasRemote) {
                  git.push().call()
            }*/
        } catch (Exception e) {
            e.printStackTrace()
            throw new IOException("Git command could not be executed", e)
        }
        return revision
    }
}
