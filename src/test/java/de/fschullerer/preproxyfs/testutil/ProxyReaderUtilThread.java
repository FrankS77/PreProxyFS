package de.fschullerer.preproxyfs.testutil;

import de.fschullerer.preproxyfs.PreProxyFSException;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a socket thread to read from socket and get input as string.
 *
 * @author Frank Schullerer
 */
public class ProxyReaderUtilThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyReaderUtilThread.class.getName());
    private final Object waitForServerSocket = new Object();
    private final Object waitForRequestReadFinished = new Object();
    private ServerSocket serverSocket;
    private String requestString;

    public ProxyReaderUtilThread() {
        // empty
    }

    public String getRequest() {
        waitForRequestToBeRead();
        return requestString;
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
        int requestLength;
        Socket clientSocket = null;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            this.serverSocket = serverSocket;
            LOGGER.info("Start ProxyReaderUtilThread on TCP port : {}", this.serverSocket.getLocalPort());
            synchronized (waitForServerSocket) {
                waitForServerSocket.notifyAll();
            }
            // We need only one message (1024byte should be enough), no loop

            clientSocket = serverSocket.accept();
            requestLength = clientSocket.getInputStream().read(request);
            if (requestLength != -1) {
                requestString = new String(request, StandardCharsets.US_ASCII).substring(0, requestLength);
            } else {
                requestString = "ERROR: no request!";
            }
            synchronized (waitForRequestReadFinished) {
                waitForRequestReadFinished.notifyAll();
            }
        } catch (BindException e) {
            throw new PreProxyFSException("Unable to bind ProxyReaderUtilThread to local port. Test exit.", e);
        } catch (SocketException e) {
            if (e.getMessage().equals("Socket closed")) {
                LOGGER.info("Closing ProxyReaderUtilThread socket");
            } else {
                throw new PreProxyFSException("Error creating ProxyReaderUtilThread socket, Test exit.", e);
            }
        } catch (IOException e) {
            throw new PreProxyFSException("Error creating ProxyReaderUtilThread socket, Test exit.", e);
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