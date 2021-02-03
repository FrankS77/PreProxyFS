package de.fschullerer.preproxyfs;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Permanent running thread. Distribute incoming requests on {@link #localBindPort} to {@link
 * ProxyForwardServer} or direct connection {@link DirectForwardServer}.
 *
 * @author Frank Schullerer
 */
public class DistributeServer extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributeServer.class.getName());
    private final Object waitForMe = new Object();
    private final Integer localBindPort;
    private ServerSocket serverSocketD;

    /**
     * Create a new DistributeServer and bind the server socket to the given port. Attention: the
     * port must not be in use.
     *
     * @param localBindPort Local bind port.
     */
    public DistributeServer(int localBindPort) {
        this.localBindPort = localBindPort;
    }

    /**
     * The server socket port was not set directly. Get it here.
     *
     * @return The server socket port number.
     */
    public Integer getPort() {
        waitForServerSocketToBeReady();
        return this.serverSocketD.getLocalPort();
    }

    private void waitForServerSocketToBeReady() {
        while (null == this.serverSocketD) {
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
     * Get server socket for e.g. closing
     *
     * @return server socket (can be null)
     */
    public ServerSocket getServerSocket() {
        waitForServerSocketToBeReady();
        return this.serverSocketD;
    }

    /**
     * Starts the distribute forward server - binds on a given port and starts serving. Create 2
     * threads for every connection requests incoming. Create one client thread to read requests
     * from client socket and send them to the correct local socket -> proxy: {@link
     * ProxyForwardServer} or direct connection {@link DirectForwardServer} The two threads live
     * only for the communication time.
     */
    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        // Bind server on given TCP port
        try (ServerSocket serverSocket = new ServerSocket(localBindPort)) {
            this.serverSocketD = serverSocket;
            LOGGER.info("Start DistributeServer on TCP port: {}", getPort());
            synchronized (waitForMe) {
                waitForMe.notifyAll();
            }
            while (true) {
                // Accept client connections and process them until stopped
                // clientSocket is closed in ClientThread
                Socket clientSocket = serverSocket.accept();
                DistributeForwardClientThread clientForward =
                        new DistributeForwardClientThread(clientSocket);
                // bind the two threads together
                ForwardServerThread serverForward = new ForwardServerThread(clientForward);
                clientForward.setForwardServerThread(serverForward);
                // start only the client thread, we don't know the remote server host name/port yet.
                clientForward.start();
            }
        } catch (BindException e) {
            throw new PreProxyFSException(
                    "Unable to bind DirectForwardServer to local port "
                            + localBindPort
                            + " Program exit.",
                    e);
        } catch (SocketException e) {
            if (e.getMessage().equals("Socket closed")) {
                LOGGER.info("Closing DistributeServer socket");
            } else {
                throw new PreProxyFSException(
                        "Error creating DistributeServer socket, Program exit.", e);
            }
        } catch (IOException e) {
            throw new PreProxyFSException(
                    "Error creating DistributeServer socket, Program exit.", e);
        }
    }
}
