package de.fschullerer.preproxyfs;

import java.io.IOException;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple forwarding thread. Forward responses to the client thread.
 *
 * @author Frank Schullerer
 */
public class ForwardServerThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForwardServerThread.class.getName());
    private final ForwardServerThreadInterface forwardClientThread;
    private Socket serverSocket;
    private final Object waitForMe = new Object();

    /**
     * Create server socket where we know the destination.
     *
     * @param forwardClientThread The client thread.
     * @param remoteHost          The remote host name.
     * @param remotePort          The remote host port.
     * @throws IOException If host is not reachable or I/O error.
     */
    public ForwardServerThread(ForwardServerThreadInterface forwardClientThread, String remoteHost, int remotePort)
            throws IOException {
        this.forwardClientThread = forwardClientThread;
        this.serverSocket = new Socket(remoteHost, remotePort);
    }

    /**
     * Create server socket where we don't know the destination yet.
     *
     * @param forwardClientThread The client thread.
     */
    public ForwardServerThread(ForwardServerThreadInterface forwardClientThread) {
        this.forwardClientThread = forwardClientThread;
    }

    /**
     * Get server socket.
     *
     * @return The server socket.
     */
    public Socket getServerSocket() {
        // waiting for other thread has set server socket
        waitForServerSocketToBeReady();
        return this.serverSocket;
    }

    private void waitForServerSocketToBeReady() {
        while (null == this.serverSocket) {
            synchronized (waitForMe) {
                try {
                    waitForMe.wait(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new PreProxyFSException("Timeout waiting for ForwardServerThread server socket.", e);
                }
            }
        }
    }
    
    /**
     * If we have created a ForwardServerThread without destination in the
     * construction, we must set the destination now. Attention: new socket will
     * only be created if server socket is null.
     *
     * @param hostName The remote host name.
     * @param port     The remote host port.
     * @throws IOException If host is not reachable or I/O error.
     */
    public void setServerSocket(String hostName, int port) throws IOException {
        if (null == this.serverSocket) {
            this.serverSocket = new Socket(hostName, port);
        }
        synchronized (waitForMe) {
            waitForMe.notifyAll();
        }
    }
    
    /**
     * Forward responses to client thread.
     */
    @Override
    public void run() {
        byte[] response;
        try {
            while (true) {
                // thread should run until end of stream is reached or exception occurs
                response = Util.readFromClientSocket(this.serverSocket);
                if (response.length == 0) {
                    break;
                }
                Util.traceLogRequestResponse(this.getClass().getName(), response);
                // forward response
                this.forwardClientThread.getClientSocket().getOutputStream().write(response);
                this.forwardClientThread.getClientSocket().getOutputStream().flush();
            }
        } catch (IOException e) {
            // Connection is broken --> exit the thread
            LOGGER.debug("Connection is broken. ", e);
        } finally {
            try {
                if (null != this.serverSocket) {
                    this.serverSocket.close();
                }
                if (null != this.forwardClientThread && null != this.forwardClientThread.getClientSocket()) {
                    this.forwardClientThread.getClientSocket().close();
                }
            } catch (IOException e) {
                LOGGER.trace("Error while closing socket.", e);
            }
        }
    }
}