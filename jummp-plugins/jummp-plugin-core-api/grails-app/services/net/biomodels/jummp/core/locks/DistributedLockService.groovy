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

import com.hazelcast.config.Config
import com.hazelcast.config.LockConfig
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.cp.lock.FencedLock
import net.biomodels.jummp.core.ILockService
import net.biomodels.jummp.core.vcs.VcsException

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * @short DistributedLockService is the means for locking resources based on
 * hazelcast distributed lock implementation.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class DistributedLockService implements ILockService {
    // locks to ensure model directories are not accessed concurrently
    private final ConcurrentHashMap<String, FencedLock> locks =
        new ConcurrentHashMap<String, FencedLock>()

    private HazelcastInstance _instance

    @PostConstruct
    private List createHzInstance() {
        Config config = new Config()
        LockConfig lockConfig = new LockConfig(name: "model-lock")
        config.addLockConfig(lockConfig)
        config.getCPSubsystemConfig().setCPMemberCount(3)
        HazelcastInstance hz1 = Hazelcast.newHazelcastInstance(config)
        HazelcastInstance hz2 = Hazelcast.newHazelcastInstance(config)
        HazelcastInstance hz3 = Hazelcast.newHazelcastInstance(config)
        [hz1, hz2, hz3] as List
    }

    @Override
    void lockModelRepository(File modelDirectory) throws VcsException {
        _instance = createHzInstance()[0]
        if (!locks.containsKey(modelDirectory.name)) {
            FencedLock lock = _instance.getCPSubsystem().getLock("model-lock")
            locks.put(modelDirectory.name, lock)
        }

//        locks.get(modelDirectory.name).tryLock(10, TimeUnit.SECONDS)
        locks.get(modelDirectory.name).lock()
    }

    @Override
    void unlockModelRepository(File modelDirectory) {
        locks.remove(modelDirectory.name)
        FencedLock lock = _instance.getCPSubsystem().getLock("model-lock")
        lock?.unlock()
    }

    @PreDestroy
    void shutdown() {
        Hazelcast.shutdownAll()
    }
}
