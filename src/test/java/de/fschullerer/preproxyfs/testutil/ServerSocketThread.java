package de.fschullerer.preproxyfs.testutil;

import de.fschullerer.preproxyfs.PreProxyFSException;
import de.fschullerer.preproxyfs.Util;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For testing purposes: Create a server socket that accepts incoming connections and send the
 * original request (as is) back to sender.
 *
 * @author Frank Schullerer
 */
public class ServerSocketThread extends Thread {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ServerSocketThread.class.getName());
    private final Object waitForServerSocket = new Object();
    private final Object waitForRequestReadFinished = new Object();
    private ServerSocket serverSocket;
    private StringBuilder messageStorage = new StringBuilder();
    private boolean acceptRequests = true;

    public ServerSocketThread() {
        // empty
    }

    /**
     * Get the messages that was received by this socket.
     *
     * @return The stored message.
     */
    public String getMessagesReceived() {
        // this thread waits until notified in run method
        waitForRequestToBeRead();
        String message = messageStorage.toString();
        // clean for new messages
        messageStorage = new StringBuilder();
        return message;
    }

    /** Close server socket. */
    public void closeSocket() {
        this.acceptRequests = false;
        if (null != serverSocket) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOGGER.trace("Error closing test socket", e);
            }
        }
    }

    /**
     * The server socket port was not set directly. Get it here.
     *
     * @return The server socket port number.
     */
    public int getPort() {
        waitForServerSocketToBeReady();
        return this.serverSocket.getLocalPort();
    }

    private void waitForServerSocketToBeReady() {
        while (null == this.serverSocket) {
            synchronized (waitForServerSocket) {
                try {
                    waitForServerSocket.wait(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new PreProxyFSException("Timeout waiting for server socket. ", e);
                }
            }
        }
    }

    private void waitForRequestToBeRead() {
        synchronized (waitForRequestReadFinished) {
            try {
                waitForRequestReadFinished.wait(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PreProxyFSException("Timeout waiting for request string to be read. ", e);
            }
        }
    }

    @Override
    public void run() {
        // Bind server on a free port
        byte[] request = new byte[1024];

        Socket clientSocket = null;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            this.serverSocket = serverSocket;
            LOGGER.info(
                    "Start ProxyReaderUtilThread on TCP port : {}",
                    this.serverSocket.getLocalPort());
            synchronized (waitForServerSocket) {
                waitForServerSocket.notifyAll();
            }
            while (acceptRequests) {
                clientSocket = serverSocket.accept();
                try {
                    request = Util.readFromClientSocket(clientSocket);
                } catch (IOException e) {
                    LOGGER.trace("Error in reading from client.", e);
                }
                if (request.length == 0) {
                    messageStorage.append("ERROR: no request!");
                } else {
                    messageStorage.append(new String(request, StandardCharsets.US_ASCII));
                }
                synchronized (waitForRequestReadFinished) {
                    waitForRequestReadFinished.notifyAll();
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Error creating ServerSocketThread server socket", e);
        } finally {
            if (null != clientSocket) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    LOGGER.trace("Error closing ProxyReaderUtilThread socket");
                }
            }
        }
    }
}
