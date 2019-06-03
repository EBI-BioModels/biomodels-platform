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
 */

package net.biomodels.jummp.core.locks

import net.biomodels.jummp.core.ILockService
import net.biomodels.jummp.core.vcs.VcsException
import org.perf4j.aop.Profiled

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * @short FileBasedLockService provides the contracts for locking resources
 * based on disk/file system.
 *
 * This class is the concrete implementation of ILockService interface. It
 * detects and locks resources which are being accessed by an individual
 * thread and unlock once that thread finishes the job.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class FileBasedLockService implements ILockService {
    // locks to ensure model directories are not accessed concurrently
    private final ConcurrentHashMap<String, ReentrantLock> locks =
        new ConcurrentHashMap<String, ReentrantLock>()
    private final ConcurrentHashMap<String, FileLock> diskLocks =
        new ConcurrentHashMap<String, FileLock>()

    @Override
    @Profiled(tag = "fileBasedLockService.lockModelRepository")
    void lockModelRepository(File modelDirectory) throws VcsException {
        if (!locks.containsKey(modelDirectory.name)) {
            ReentrantLock lock = new ReentrantLock()
            locks.put(modelDirectory.name, lock)
        }
        locks.get(modelDirectory.name).lock()
        FileLock fileLock = obtainExclusiveLock(modelDirectory)
        diskLocks.put(modelDirectory.name, fileLock)
    }

    @Override
    @Profiled(tag = "fileBasedLockService.unlockModelRepository")
    void unlockModelRepository(File modelDirectory) {
        ReentrantLock lock = locks.get(modelDirectory.name)
        if (!lock.hasQueuedThreads()) locks.remove(modelDirectory)
        new File(modelDirectory, ".git/.locker.txt").setText("")
        FileLock removing = diskLocks.remove(modelDirectory.name)
        removing.release()
        removing.channel().close()
        lock.unlock()
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
                accumulate += 10000
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

    private FileChannel getRepositoryChannel(File modelDirectory) {
        File repositoryFile = new File(modelDirectory, ".git/.locker.txt")
        FileChannel channel = new RandomAccessFile(repositoryFile, "rw").getChannel()
        return channel
    }
}
