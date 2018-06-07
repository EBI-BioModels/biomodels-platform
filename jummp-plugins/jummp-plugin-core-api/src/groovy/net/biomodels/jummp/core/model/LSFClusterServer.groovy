package net.biomodels.jummp.core.model

class LSFClusterServer {
    private String hostName
    private int port

    String getHostName() {
        return hostName
    }

    void setHostName(String hostName) {
        this.hostName = hostName
    }

    int getPort() {
        return port
    }

    void setPort(int port) {
        this.port = port
    }
}
