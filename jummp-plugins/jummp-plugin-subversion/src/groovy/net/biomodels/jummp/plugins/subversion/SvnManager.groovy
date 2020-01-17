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
* Apache Commons, Svnkit (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, TMate Open Source License, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Commons, Svnkit used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.plugins.subversion

import net.biomodels.jummp.core.vcs.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import org.apache.commons.io.FileUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.tmatesoft.svn.core.SVNCommitInfo
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl

/**
 * @short SvnManager provides the interface to a working copy of an Subversion checkout.
 *
 * This class is the concrete implementation of the VcsManager for Subversion (SVN). It manages checking out
 * a working copy, importing files, updating files and retrieving files.
 * It uses the high level API of SvnKit for managing the working copy. The low level API is not used, that is
 * it does not create the repository itself and needs to checkout a working copy.
 *
 * SvnManager uses a ReentrantLock in all of its method to make it thread safe. Nevertheless this does not ensure
 * that the working copy is kept in a correct state. If two SvnManager access the same working copy, the lock cannot
 * protect and SvnManager is also not able to detect whether the working copy has been changed outside the class.
 * It is important to let the instance of the SvnManager be the only resource accessing the working copy!
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
public class SvnManager implements VcsManager {
    // TODO: we need some way of authentication
    private static final ReentrantLock lock = new ReentrantLock()
    private static final AtomicInteger uid = new AtomicInteger(0)
    private static final Log log = LogFactory.getLog(this)
    /**
     * Whether SvnManager is correctly inited or not
     */
    private boolean inited = false
    private SVNClientManager manager
    /**
     * The url of the remote (or local) SVN repository
     */
    private SVNURL repositoryUrl
    /**
     * Directory containing the working copy
     */
    private File workingCopy
    /**
     * Directory for exchanging retrieved files from the SVN repository
     */
    private File exchangeDirectory

    private SvnManager() {
        setupLibrary()
    }

    /**
     * Constructor for an SvnManager connecting to a local repository
     * @param locaRepo File of the local repository
     */
    public SvnManager(File localRepo) {
        this()
        try {
            repositoryUrl = SVNURL.fromFile(localRepo)
            manager = SVNClientManager.newInstance()
        } catch (SVNException e) {
            log.error(e.message, e)
        }
    }

    /**
     * Constructor for an SvnManager connecting to a remote repository
     * @param uri The uri for the remote repository - e.g svn+ssh://user@svn.example.org/home/svn/trunk/repository
     */
    public SvnManager(String uri) {
        this()
        try {
            repositoryUrl = SVNURL.parseURIDecoded(uri)
            manager = SVNClientManager.newInstance()
        } catch (SVNException e) {
            throw new VcsException(e)
        }
    }

    public void init(File workingCopy, File exchangeDirectory) {
        lock.lock()
        try {
            if (inited) {
                throw new VcsAlreadyInitedException()
            }
            if (!workingCopy.isDirectory() || !workingCopy.exists()) {
                throw new VcsException("Working copy directory " + workingCopy.toString() + " is either not a directory or does not exist")
            }
            if (workingCopy.list().length > 0) {
                throw new VcsException("Working copy directory " + workingCopy.toString() + " is not empty")
            }
            if (!exchangeDirectory.isDirectory() || !exchangeDirectory.exists()) {
                throw new VcsException("Exchange directory " + exchangeDirectory.toString() + " is either not a directory or does not exist")
            }
            this.workingCopy = workingCopy
            this.exchangeDirectory = exchangeDirectory
            try {
                manager.getUpdateClient().doCheckout(repositoryUrl, workingCopy, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.INFINITY, false)
            } catch (SVNException e) {
                throw new VcsException(e)
            }
            inited = true
        } finally {
            lock.unlock()
        }
    }

    public String importFile(File file, String name, String commitMessage) {
        String revision
        lock.lock()
        try {
            if (!inited) {
                throw new VcsNotInitedException()
            }
            if (!file.exists()) {
                throw new VcsException(file.toString() + " does not exists and cannot be imported into SVN")
            }
            if (!file.isFile()) {
                throw new VcsException(file.toString() + " is not a file and cannot be imported into SVN")
            }
            if (workingCopy.list().toList().contains(name)) {
                throw new FileAlreadyVersionedException(name)
            }
            File destination = new File(workingCopy.absolutePath + System.getProperty("file.separator") + name)
            FileUtils.copyFile(file, destination)
            File[] fileArray = new File[1]
            fileArray[0] = destination
            try {
                manager.getWCClient().doAdd(destination, false, false, false, SVNDepth.FILES, false, false)
                SVNCommitInfo info = manager.getCommitClient().doCommit(fileArray, false, commitMessage, null, null, false, false, SVNDepth.FILES)
                revision = String.valueOf(info.getNewRevision())
            } catch (SVNException e) {
                throw new VcsException(e)
            }
        } finally {
            lock.unlock()
        }
        return revision
    }

    public String importFile(File file, String name) {
        return importFile(file, name, "Import of ${name}")
    }

    public String updateFile(File file, String name, String commitMessage) {
        String revision
        lock.lock()
        try {
            if (!inited) {
                throw new VcsNotInitedException()
            }
            if (!file.exists()) {
                throw new VcsException(file.toString() + " does not exists and cannot be imported into SVN")
            }
            if (!file.isFile()) {
                throw new VcsException(file.toString() + " is not a file and cannot be imported into SVN")
            }
            if (!workingCopy.list().toList().contains(name)) {
                throw new FileNotVersionedException(name)
            }
            File destination = new File(workingCopy.absolutePath + System.getProperty("file.separator") + name)
            FileUtils.copyFile(file, destination)
            File[] fileArray = new File[1]
            fileArray[0] = destination
            try {
                SVNCommitInfo info = manager.getCommitClient().doCommit(fileArray, false, commitMessage, null, null, false, false, SVNDepth.FILES)
                revision = String.valueOf(info.getNewRevision())
            } catch (SVNException e) {
                throw new VcsException(e)
            }
        } finally {
            lock.unlock()
        }
        return revision
    }

    public String updateFile(File file, String name) {
        return updateFile(file, name, "Update of ${name}")
    }

    public File retrieveFile(String file, String revision) {
        File destinationFile
        lock.lock()
        try {
            if (!inited) {
                throw new VcsNotInitedException()
            }
            if (!workingCopy.list().toList().contains(file)) {
                throw new FileNotVersionedException(file)
            }
            long rev
            try {
                if (revision) {
                    rev = Long.parseLong(revision)
                }
            } catch (NumberFormatException e) {
                throw new VcsException("${revision} is not a valid SVN revision")
            }
            File sourceFile = new File(workingCopy.absolutePath + System.getProperty("file.separator") + file)
            destinationFile = new File(exchangeDirectory.absolutePath + System.getProperty("file.separator") + "svn_${uid.getAndIncrement()}_" + file)
            if (revision == null) {
                // return current HEAD revision
                FileUtils.copyFile(sourceFile, destinationFile)
            } else {
                // getting an older revision
                try {
                    manager.getUpdateClient().doExport(sourceFile, destinationFile, SVNRevision.UNDEFINED, SVNRevision.create(rev), null, true, SVNDepth.EMPTY)
                } catch (SVNException e) {
                    throw new VcsException(e)
                }
            }
        } finally {
            lock.unlock()
        }
        return destinationFile
    }

    public File retrieveFile(String file) {
        return retrieveFile(file, null)
    }

    public void updateWorkingCopy() {
        lock.lock()
        try {
            if (!inited) {
                throw new VcsNotInitedException()
            }
            try {
                manager.getUpdateClient().doUpdate(workingCopy, SVNRevision.HEAD, SVNDepth.INFINITY, false, false)
            } catch (SVNException e) {
                throw new VcsException(e)
            }
        } finally {
            lock.unlock()
        }
    }

    void init(File d) {
        throw new UnsupportedOperationException("This is a stub.")
    }

    String updateModel(File d, List files, String msg) {
        throw new UnsupportedOperationException("This is a stub.")
    }

    List getRevisions(File d) {
        throw new UnsupportedOperationException("This is a stub.")
    }

    List retrieveModel(File d) {
        throw new UnsupportedOperationException("This is a stub.")
    }

    String createModel(File d, List f, String msg) {
        throw new UnsupportedOperationException("This is a stub.")
    }

    String updateModel(File d, List f) {
        throw new UnsupportedOperationException("This is a stub.")
    }

    List retrieveModel(File d, String revisionId) {
        throw new UnsupportedOperationException("This is a stub.")
    }

    void updateWorkingCopy(File d){
        throw new UnsupportedOperationException("This is a stub.")
    }

    @Override
    void resetModelRepository(File modelDirectory, String commitId) throws VcsException {
        throw new UnsupportedOperationException("Not implemented yet")
    }
/*
     * Initializes the library to work with a repository via
     * different protocols.
     */
    private static void setupLibrary() {
        /*
         * For using over http:// and https://
         */
        DAVRepositoryFactory.setup()
        /*
         * For using over svn:// and svn+xxx://
         */
        SVNRepositoryFactoryImpl.setup()
        /*
         * For using over file:///
         */
        FSRepositoryFactory.setup()
        log.debug("SVNKit setup complete.")
    }
}
