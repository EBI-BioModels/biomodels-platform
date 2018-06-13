package net.biomodels.jummp.utils

import net.biomodels.jummp.exception.network.NetworkUnreachableException

class NetworkUtils {

    private static int WAIT_TIME = 2000

    private static int MILLISECONDS = 1000

    static boolean isReachable(String host, int port) {
        SocketAddress sockaddr = new InetSocketAddress(host, port)
        Socket socket = new Socket()
        boolean online = true
        // Connect with 60 s timeout
        try {
            socket.connect(sockaddr, 60000)
        } catch (SocketTimeoutException stex) {
            // treating timeout errors separately from other io exceptions
            // may make sense
            online = false
        } catch (IOException iOException) {
            online = false
        } finally {
            // As the close() operation can also throw an IOException
            // it must caught here
            socket.close()
        }
        return online
    }

    /**
     * Wait until the service ready
     * @param host
     * @param port
     * @param maxtime maximum time to wait (in seconds). Beyond this time the service consider as unreachable
     */
    static void waitUntilServiceReady(String host, int port, int maxtime) {
        int nTries = (int) Math.ceil(maxtime * MILLISECONDS / (double)WAIT_TIME)
        int current = 0
        while (!isReachable(host, port)) {
            if (current++ > nTries) {
                throw new NetworkUnreachableException(String.format("Can't connect %s:%d", host, port))
            }
            JummpUtils.sleep(WAIT_TIME)
        }
    }
}
