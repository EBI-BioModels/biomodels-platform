package net.biomodels.jummp.utils

class NetworkUtils {
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
}
