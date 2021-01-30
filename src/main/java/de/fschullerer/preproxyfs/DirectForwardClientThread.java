package de.fschullerer.preproxyfs;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DirectForwardClientThread reads one request that was sent to the {@link DirectForwardServer}
 * server socket (e.g. port 65001), creates a new connection to a remote server (e.g. google.com:443),
 * starts a new {@link ForwardServerThread} that will process the response from that that server and
 * forward the original request to the remote server.
 *
 * @author Frank Schullerer
 */
public class DirectForwardClientThread extends Thread implements ForwardServerThreadInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectForwardClientThread.class.getName());

    private final Socket clientSocket;
    private ForwardServerThread directForwardServerThread;

    /**
     * Creates a new direct forwarding thread.
     */
    DirectForwardClientThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * Bind this DirectForwardClientThread to a forwarding server thread. So it is possible
     * to get the server socket.
     *
     * @param directForwardServerThread Bind ForwardServerThread to this object.
     */
    void setForwardServerThread(ForwardServerThread directForwardServerThread) {
        this.directForwardServerThread = directForwardServerThread;
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
     * If reading or writing can not be done (due to exception or
     * when the stream is at his end) or writing is failed, exits the thread.
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

                // US_ASCII !!!! not UTF-8 !!
                String httpReq = new String(request, StandardCharsets.US_ASCII);
                // communication should always start with CONNECT (proxy connection) and then GET, POST ...
                if (httpReq.startsWith("CONNECT")) {
                    byte[] connectionEstablished = Util.CONNECTION_ESTABLISHED.getBytes(StandardCharsets.US_ASCII);
                    // directly write the Connection established response to the client socket because the
                    // server socket (a remote server) does not understand a CONNECT request if it is not
                    // a proxy. CONNECT requests are only for proxy connections.
                    clientSocket.getOutputStream().write(connectionEstablished);
                    // We need the host name and port number from the CONNECT request to forward the request
                    // to the correct server
                    String url = Util.getUrl(httpReq);
                    String[] urlSplit = url.split(":");
                    String hostFromUrl = "";
                    int portFromUrl = 443;
                    if (urlSplit.length > 0) {
                        hostFromUrl = urlSplit[0];
                        portFromUrl = Integer.parseInt(urlSplit[1]);
                    }
                    // set parent server socket with remote server port
                    // socket will be closed in thread
                    this.directForwardServerThread.setServerSocket(hostFromUrl, portFromUrl);
                    // start server thread after we set the server socket
                    this.directForwardServerThread.start();

                } else {
                    // not a CONNECT request -> forward the request 
                    this.directForwardServerThread.getServerSocket().getOutputStream().write(request);
                    this.directForwardServerThread.getServerSocket().getOutputStream().flush();
                }
            }
        } catch (Exception e) {
            // Connection is broken --> exit the thread
            LOGGER.debug("Connection is broken: ", e);
        } finally {
            try {
                if (null != this.clientSocket) {
                    this.clientSocket.close();
                }
                if (null != this.directForwardServerThread
                        && null != this.directForwardServerThread.getServerSocket()) {
                    this.directForwardServerThread.getServerSocket().close();
                }
            } catch (IOException e) {
                LOGGER.trace("Error while closing socket: ", e);
            }
        }
    }
}