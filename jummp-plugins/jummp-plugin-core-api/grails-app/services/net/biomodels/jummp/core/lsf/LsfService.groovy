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
import net.biomodels.jummp.core.model.LSFApplication
import net.biomodels.jummp.utils.FileUtils
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
class LsfService implements InitializingBean {

    static transactional = false
    private static final int SESSION_TIMEOUT = 60 * 60 * 1000

    private static final int CHUNK_SIZE = 1024

    private static final int READ_TIMEOUT = 1000

    private static final int WAIT_FOR_CLUSTER_READY = 5000

    private static final int JOB_PID_LENGTH = 10

    private static final int MIN_PORT = 10000

    private static final int MAX_PORT = 99999

    private static final Logger LOGGER = LoggerFactory.getLogger(LsfService.class)

    /**
     * SSH Manager
     */
    private JSch jSch = new JSch()

    private String lsfMiddlewareHost

    private String lsfMiddlewareUsername

    private String lsfMiddlewarePassword

    private String lsfDefaultQueue

    private String lsfOutputDir

    /**
     * Location of the application folder
     */
    private String lsfApplicationPath

    /**
     * LFS Cluster machine connections
     */
    private Map<String, Session> connections = new ConcurrentHashMap<>()

    /**
     * Mapping between jobId and PID
     */
    private Map<String, String> jobPids = new ConcurrentHashMap<>()

    /**
     * Mapping between job and Application
     */
    private Map<String, LSFApplication> jobApplication = new HashMap<>()

    /**
     * Dependency Injection of GrailsApplication
     */
    def grailsApplication

    void afterPropertiesSet() throws Exception {
        lsfMiddlewareHost = grailsApplication.config.jummp.lsf.middleware.host
        lsfMiddlewareUsername = grailsApplication.config.jummp.lsf.middleware.username
        lsfMiddlewarePassword = grailsApplication.config.jummp.lsf.middleware.password
        lsfApplicationPath = grailsApplication.config.jummp.lsf.application.path
        lsfDefaultQueue = grailsApplication.config.jummp.lsf.queue.default
        lsfOutputDir = grailsApplication.config.jummp.lsf.output.dir
        jSch.addIdentity(grailsApplication.config.jummp.lsf.privatekey)
    }



    /**
     * Create a connect to LFS cluster and wait until job started
     * @param application Application need to be deployed
     * @param nRam number of RAM required
     * @param nCpu number of CPU required
     * @param maxTime maximum time allowed to run the application
     * @return String job id
     */
    synchronized String startLFSClusterJob(LSFApplication application, int nRam, int nCpu, int maxTime) {
        String jobPid = "JOB_" + JummpUtils.randStr(JOB_PID_LENGTH)
        int port = JummpUtils.randInt(MIN_PORT, MAX_PORT)
        Session session = jSch.getSession(lsfMiddlewareUsername, lsfMiddlewareHost)
        session.setConfig("StrictHostKeyChecking", "no")
        session.connect(SESSION_TIMEOUT)
        List<String> command = new ArrayList<>()
        command.add("bsub")
        command.add(String.format("-q %s", lsfDefaultQueue))
        command.add(String.format("-M %d", nRam))
        command.add(String.format("-R \"rusage[mem=%d]\"", nRam))
        command.add(String.format("-n %d", nCpu))
        command.add(String.format("-o %s/%%J.log", lsfOutputDir))
        command.add(lsfApplicationPath + "/" + application.getName() + "/" + application.getStartScript())
        command.add(jobPid)
        command.add(Integer.toString(maxTime))
        command.add(Integer.toString(port))
        String response = executeCommand(session, String.join(" ", command))
        Pattern pattern = Pattern.compile("Job\\s+<(\\d+)>")
        Matcher matcher = pattern.matcher(response)
        if (matcher.find()) {
            String jobId = matcher.group(1)
            command.clear()
            command.add("bjobs")
            command.add(String.format("-o \"stat: exec_host\""))
            command.add(jobId)
            command.add("-noheader")
            while (true) {
                response = executeCommand(session, String.join(" ", command))
                if (response.split(" ")[0] == "RUN") {
                    String host = response.split("\\s+")[1]
                    if (host.contains("*")) {
                        host = host.split('\\*')[1]
                    }
                    Session machineSession = jSch.getSession(lsfMiddlewareUsername, host)
                    machineSession.setConfig("StrictHostKeyChecking", "no")
                    machineSession.connect(SESSION_TIMEOUT)
                    connections.put(jobId, machineSession)
                    jobPids.put(jobId, jobPid)
                    jobApplication.put(jobId, application)
                    break
                } else if (response.split(" ")[0] == "PEND" || response.split(" ")[0] == "WAIT") {
                    JummpUtils.sleep(WAIT_FOR_CLUSTER_READY)
                } else {
                    String logPath = String.format("%s/%s.log", lsfOutputDir, jobId)
                    String logOutput = FileUtils.readLSFOutput(logPath)
                    if (logOutput != null && logOutput == "Port unavailable!") {
                        session.disconnect()
                        // Try again with different port
                        return startLFSClusterJob(application, nRam, nCpu, maxTime)
                    }
                    LOGGER.error("An exception occurred when run job, command {}, status {}", command, response)
                    throw new RuntimeException("An exception occurred when run job")
                }
            }
            session.disconnect()
            return jobId
        }
        LOGGER.error("Can't submit job to LSF Cluster, command {}, output {}", command, response)
        throw new RuntimeException("Can't submit job to LSF Cluster")
    }

    /**
     * Run the command on LSF Cluster machine
     * @param session
     * @param command
     * @return
     */
    private String executeCommand(Session session, String command) {
        Channel channel=session.openChannel("exec")
        ((ChannelExec)channel).setCommand(String.join(" ", command))
        channel.setInputStream(null)
        ((ChannelExec)channel).setErrStream(System.err)
        InputStream inputStream = channel.getInputStream()
        byte[] tmp = new byte[CHUNK_SIZE]
        StringBuilder stringBuilder = new StringBuilder()
        channel.connect()
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
                    throw new RuntimeException("Exception occurred during execute command")
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
        return connections.get(jobId).getHost()
    }

    /**
     * Get port of the application that running in the LSF Cluster and have the given job ID
     * @param jobId
     * @return
     */
    int getPortByJobId(String jobId) {
        return connections.get(jobId).getPort()
    }

    /**
     * Stop the application running under LFS Cluster, which have the given job ID
     * @param jobId
     */
    synchronized void stopLFSClusterJob(String jobId) {
        Session session = connections.get(jobId)
        List<String> command = new ArrayList<>()

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
