package net.biomodels.jummp.core.model

import com.jcraft.jsch.Session

class LSFClusterJob {
    private String jobId
    private LSFClusterServer server
    private LSFApplication application
    private String pidFile
    private Session session

    LSFClusterJob(String jobId, String hostName, int port, LSFApplication application, String pidFile, Session session) {
        this.jobId = jobId
        server = new LSFClusterServer()
        server.port = port
        server.hostName = hostName
        this.application = application
        this.pidFile = pidFile
        this.session = session
    }

    String getJobId() {
        return jobId
    }

    String getHostName() {
        return server.hostName
    }

    int getPort() {
        return server.port
    }

    LSFApplication getApplication() {
        return application
    }

    String getPidFile() {
        return pidFile
    }

    Session getSession() {
        return session
    }
}
