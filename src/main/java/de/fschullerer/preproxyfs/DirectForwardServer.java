package de.fschullerer.preproxyfs;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Permanent running thread. The DirectForwardServer will handle all requests coming from the {@link
 * DistributeForwardClientThread} to serve direct connections (without proxies) to remote servers.
 *
 * @author Frank Schullerer
 */
public class DirectForwardServer extends Thread {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DirectForwardServer.class.getName());
    // only one DirectForwardServer TODO singleton?
    private ServerSocket serverSocketD;
    private final Object waitForMe = new Object();

    /**
     * Create a new DirectForwardServer and bind the server socket to the given port. Attention: A
     * random free local port is chosen. Get it with {@link #getPort()}
     */
    public DirectForwardServer() {
        // empty
    }

    /**
     * The server socket port was not set directly. Get it here.
     *
     * @return The server socket port number.
     */
    public int getPort() {
        waitForServerSocketToBeReady();
        return this.serverSocketD.getLocalPort();
    }

    /**
     * Get server socket for e.g. closing
     *
     * @return server socket (can be null)
     */
    ServerSocket getServerSocketD() {
        waitForServerSocketToBeReady();
        return this.serverSocketD;
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
     * Starts the direct forward server - binds on a given port and starts serving. Create 2 threads
     * for every connection requests incoming. Create one client thread to read requests from client
     * socket and send them to remote server. Create forward thread to read responses from remote
     * server (server socket) and forward them to the client (client socket). The two threads live
     * only for the communication time.
     */
    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        // Bind server on a free port
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            this.serverSocketD = serverSocket;
            LOGGER.info(
                    "Start DirectForwardServer on TCP port : {}",
                    this.serverSocketD.getLocalPort());
            synchronized (waitForMe) {
                waitForMe.notifyAll();
            }
            // Accept client connections and process them until stopped
            while (true) {
                Socket clientSocket = serverSocket.accept();
                DirectForwardClientThread clientForward =
                        new DirectForwardClientThread(clientSocket);
                ForwardServerThread serverForward = new ForwardServerThread(clientForward);
                // bind the two threads together
                clientForward.setForwardServerThread(serverForward);
                // start only the client thread, we don't know the remote server host name/port yet.
                clientForward.start();
            }
        } catch (BindException e) {
            throw new PreProxyFSException(
                    "Unable to bind DirectForwardServer to local port. Program exit.", e);
        } catch (SocketException e) {
            if (e.getMessage().equals("Socket closed")) {
                LOGGER.info("Closing DistributeServer socket");
            } else {
                throw new PreProxyFSException(
                        "Error creating DirectForwardServer socket, Program exit.", e);
            }
        } catch (IOException e) {
            throw new PreProxyFSException(
                    "Error creating DirectForwardServer socket, Program exit.", e);
        }
    }
}
