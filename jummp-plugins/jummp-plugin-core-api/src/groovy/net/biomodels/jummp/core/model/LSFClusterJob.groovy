package net.biomodels.jummp.core.model

import com.jcraft.jsch.Session

class LSFClusterJob {
    private String jobId
    private String hostName
    private int port
    private LSFApplication application
    private String pidFile
    private Session session

    LSFClusterJob(String jobId, String hostName, int port, LSFApplication application, String pidFile, Session session) {
        this.jobId = jobId
        this.hostName = hostName
        this.port = port
        this.application = application
        this.pidFile = pidFile
        this.session = session
    }

    String getJobId() {
        return jobId
    }

    String getHostName() {
        return hostName
    }

    int getPort() {
        return port
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
