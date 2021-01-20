package de.fschullerer.preproxyfs;

import java.io.IOException;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple forwarding thread.Forward the client request without modification to
 * the remote proxy.
 *
 * @author Frank Schullerer
 */
public class ProxyForwardClientThread extends Thread implements ForwardServerThreadInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyForwardClientThread.class.getName());
    private final Socket clientSocket;
    private ForwardServerThread proxyForwardServerThread;

    /**
     * Creates a new proxy forwarding thread.
     */
    ProxyForwardClientThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * Bind this ProxyForwardClientThread to a forwarding server thread. So it is
     * possible to get the server socket.
     *
     * @param proxyForwardServerThread Bind this object with ForwardServerThread.
     */
    void setForwardServerThread(ForwardServerThread proxyForwardServerThread) {
        this.proxyForwardServerThread = proxyForwardServerThread;
    }

    /**
     * Get the client socket. The server thread needs it.
     */
    @Override
    public Socket getClientSocket() {
        return this.clientSocket;
    }

    /**
     * Read from client socket and write to the server socket until it is possible.
     */
    @Override
    public void run() {
        byte[] request;
        try {
            while (true) {
                // thread should run until end of stream is reached or exception occurs
                request = Util.readFromClientSocket(this.clientSocket);
                if (request.length == 0) {
                    break;
                }
                Util.traceLogRequestResponse(this.getClass().getName(), request);
                this.proxyForwardServerThread.getServerSocket().getOutputStream().write(request);
                this.proxyForwardServerThread.getServerSocket().getOutputStream().flush();
            }
        } catch (Exception e) {
            // Connection is broken --> exit the thread
            LOGGER.debug("Connection is broken.", e);
        } finally {
            // Notify parent thread that the connection is broken and forwarding should stop
            try {
                if (null != this.clientSocket) {
                    this.clientSocket.close();
                }
                if (null != this.proxyForwardServerThread && null != this.proxyForwardServerThread.getServerSocket()) {
                    this.proxyForwardServerThread.getServerSocket().close();
                }
            } catch (IOException e) {
                LOGGER.trace("Error while closing socket.", e);
            }
        }
    }
}