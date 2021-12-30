package de.fschullerer.preproxyfs;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Permanent running thread. The ProxyForwardServer will handle all requests coming from the {@link
 * DistributeForwardClientThread} to serve proxy connections to remote servers.
 *
 * @author Frank Schullerer
 */
public class ProxyForwardServer extends Thread {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProxyForwardServer.class.getName());
    private final Object waitForMe = new Object();
    private final String remoteProxyHost;
    private final int remoteProxyPort;
    private ServerSocket serverSocketP;

    /**
     * This will create a ProxyForwardServer to a concrete destination (a remote proxy). The local
     * bind port will be set automatically (a free random port)
     *
     * @param remoteProxyHost The remote proxy hostname/IP.
     * @param remoteProxyPort The remote proxy port.
     */
    public ProxyForwardServer(String remoteProxyHost, int remoteProxyPort) {
        this.remoteProxyHost = remoteProxyHost;
        this.remoteProxyPort = remoteProxyPort;
    }

    /**
     * The server socket port was not set directly. Get it here.
     *
     * @return The server socket port number.
     */
    public int getPort() {
        waitForServerSocketToBeReady();
        return this.serverSocketP.getLocalPort();
    }

    /**
     * Get server socket for e.g. closing.
     *
     * @return server socket (can be null)
     */
    public ServerSocket getServerSocketP() {
        waitForServerSocketToBeReady();
        return this.serverSocketP;
    }

    private void waitForServerSocketToBeReady() {
        while (null == this.serverSocketP) {
            synchronized (waitForMe) {
                try {
                    waitForMe.wait(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new PreProxyFSException(
                            "Timeout waiting for server socket. Should not happen.", e);
                }
            }
        }
    }

    /**
     * Get remote proxy e.g. remoteproxy1.com:8080
     *
     * @return Proxy
     */
    String getProxy() {
        return this.remoteProxyHost + ":" + this.remoteProxyPort;
    }

    /**
     * Starts the direct forward server - binds on a given port and starts serving. Create 2 threads
     * for every connection requests incoming. Create one client thread to read requests from client
     * socket and send them to remote server. Create forward thread to read responses from remote
     * server and forward them to the client (client socket). The two threads live only for the
     * communication time.
     */
    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        // Bind server on given TCP port
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            // set hard connection timeout
            serverSocket.setSoTimeout(Util.SOCKET_CONN_TIMEOUT);
            this.serverSocketP = serverSocket;
            LOGGER.info(
                    "Start ProxyForwardServer on TCP port: {} . Connected"
                            + " to remote proxy: {}:{}",
                    this.serverSocketP.getLocalPort(),
                    remoteProxyHost,
                    remoteProxyPort);
            synchronized (waitForMe) {
                waitForMe.notifyAll();
            }
            // Accept client connections and process them until stopped
            while (true) {
                // clientSocket is closed in ClientThread
                Socket clientSocket = serverSocket.accept();
                ProxyForwardClientThread clientForward = new ProxyForwardClientThread(clientSocket);
                // bind the two threads together
                ForwardServerThread serverForward =
                        new ForwardServerThread(
                                clientForward, this.remoteProxyHost, this.remoteProxyPort);
                clientForward.setForwardServerThread(serverForward);
                clientForward.start();
                serverForward.start();
            }
        } catch (BindException e) {
            throw new PreProxyFSException(
                    "Unable to bind ProxyForwardServer to local port. Program exit.", e);
        } catch (SocketException e) {
            if (e.getMessage().equals("Socket closed")) {
                LOGGER.info("Closing DistributeServer socket");
            } else {
                throw new PreProxyFSException(
                        "Error creating ProxyForwardServer socket, Program exit.", e);
            }
        } catch (IOException e) {
            throw new PreProxyFSException(
                    "Error creating ProxyForwardServer socket, Program exit.", e);
        }
    }
}
