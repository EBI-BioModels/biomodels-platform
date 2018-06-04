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
 **/



package net.biomodels.jummp.core.lsf

import net.biomodels.jummp.core.model.LFSApplication


/**
 * This service have the responsibility to connect, maintain and release the connection
 * to the LFS cluster machine
 *
 * @author  Tu Vu <tvu@ebi.ac.uk>
 */
class LFSService {

    static transactional = false

    /**
     * Create a connect to LFS cluster and wait until job started
     * @param application Application need to be deployed
     * @param nRam number of RAM required
     * @param nCpu number of CPU required
     * @return String job id
     */
    String startLFSClusterJob(LFSApplication application, int nRam, int nCpu) {
        return null
    }

    /**
     * Get IP of the machine in LFS Cluster, which running the given job ID
     * @param jobId
     * @return IP of the machine
     */
    String getIPByJobId(String jobId) {
        return null
    }

    /**
     * Stop the application running under LFS Cluster, which have the given job ID
     * @param jobId
     */
    void stopLFSClusterJob(String jobId) {

    }

    /**
     * Move the data of the application from the LFS Cluster machine to the server
     * @param jobId
     * @param lfsClusterDataPathFrom
     * @param serverPathTo
     */
    void moveData(String jobId, String lfsClusterDataPathFrom, String serverPathTo) {

    }
}
