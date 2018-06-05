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

import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import net.biomodels.jummp.core.model.LFSApplication
import net.biomodels.jummp.utils.JummpUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean

import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * This service have the responsibility to connect, maintain and release the connection
 * to the LFS cluster machine
 *
 * @author  Tu Vu <tvu@ebi.ac.uk>
 */
class LFSService implements InitializingBean {

    static transactional = false
    private static final int SESSION_TIMEOUT = 60 * 60 * 1000

    private static final int CHUNK_SIZE = 1024

    private static final int READ_TIMEOUT = 1000

    private static final int WAIT_FOR_CLUSTER_READY = 5000

    private static final Logger LOGGER = LoggerFactory.getLogger(LFSService.class)

    /**
     * SSH Manager
     */
    private JSch jSch = new JSch()

    private String lfsMiddlewareHost

    private String lfsMiddlewareUsername

    private String lfsMiddlewarePassword

    private String lfsDefaultQueue

    /**
     * Location of the application folder
     */
    private String lfsApplicationPath

    /**
     * LFS Cluster machine connections
     */
    private Map<String, Session> connections = new ConcurrentHashMap<>()

    /**
     * Dependency Injection of GrailsApplication
     */
    def grailsApplication

    void afterPropertiesSet() throws Exception {
        lfsMiddlewareHost = grailsApplication.config.jummp.lfs.middleware.host
        lfsMiddlewareUsername = grailsApplication.config.jummp.lfs.middleware.username
        lfsMiddlewarePassword = grailsApplication.config.jummp.lfs.middleware.password
        lfsApplicationPath = grailsApplication.config.jummp.lfs.application.path
        lfsDefaultQueue = grailsApplication.config.jummp.lfs.queue.default
    }

    /**
     * Create a connect to LFS cluster and wait until job started
     * @param application Application need to be deployed
     * @param nRam number of RAM required
     * @param nCpu number of CPU required
     * @return String job id
     */
    String startLFSClusterJob(LFSApplication application, int nRam, int nCpu) {
        return null
    private String executeCommand(Session session, String command) {
        Channel channel=session.openChannel("exec")
        ((ChannelExec)channel).setCommand(String.join(" ", command))
        channel.setInputStream(null)
        ((ChannelExec)channel).setErrStream(System.err)
        InputStream inputStream = channel.getInputStream()
        byte[] tmp = new byte[CHUNK_SIZE]
        StringBuilder stringBuilder = new StringBuilder()
        while(true) {
            while(inputStream.available() > 0) {
                int i = inputStream.read(tmp, 0, CHUNK_SIZE)
                if (i < 0) {
                    break
                }
                stringBuilder.append(new String(tmp, 0, i))
            }
            if (channel.isClosed()) {
                if(inputStream.available() > 0) {
                    continue
                }
                if (channel.getExitStatus() != 0) {
                    LOGGER.error("Exception during execute command {}, {}", command, stringBuilder)
                    throw RuntimeException("Exception occurred during execute command")
                }
                break
            }
            JummpUtils.sleep(READ_TIMEOUT)
        }
        channel.disconnect()
        return stringBuilder.toString()
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
