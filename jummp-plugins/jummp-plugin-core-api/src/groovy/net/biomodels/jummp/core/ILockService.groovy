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

package net.biomodels.jummp.core

import net.biomodels.jummp.core.vcs.VcsException

/**
 * @short Contract of services for dealing with locking implementations.
 *
 * Implementations of this interface must provide means for locking
 * commonly used resources in both local and distributed manner.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
interface ILockService {
    /**
     * Lock a model repository
     *
     * Checks whether a lock for the directory exists. If it does not exist
     * then it associates a lock with the model directory. It then acquires
     * the model directory lock
     * @param modelDirectory The directory to lock
     */
    void lockModelRepository(File modelDirectory) throws VcsException

    /**
     * Unlock a model repository
     *
     * Unlocks the model directory, and if no other threads are waiting on it
     * the lock is removed from the locks container
     * @param modelDirectory The directory to unlock
     */
    void unlockModelRepository(File modelDirectory) throws VcsException
}
