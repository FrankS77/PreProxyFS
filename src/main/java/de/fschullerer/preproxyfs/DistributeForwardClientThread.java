package de.fschullerer.preproxyfs;

import com.github.markusbernhardt.proxy.selector.pac.ProxyEvaluationException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This thread will distribute all requests coming to the PreProxyFS#mainPort to
 * the defined proxies (or direct connection) in the PAC script.
 *
 * @author Frank Schullerer
 */
public class DistributeForwardClientThread extends Thread implements ForwardServerThreadInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributeForwardClientThread.class.getName());
    private final Socket clientSocket;
    private ForwardServerThread distributeForwardServerThread;

    /**
     * Creates a new distribution thread.
     */
    public DistributeForwardClientThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * Bind this DistributeForwardClientThread to a forwarding server thread. So it
     * is possible to get the server socket.
     *
     * @param distributeForwardServerThread Bind with ForwardServerThread.
     */
    public void setForwardServerThread(ForwardServerThread distributeForwardServerThread) {
        this.distributeForwardServerThread = distributeForwardServerThread;
    }

    /**
     * Get the client socket. The server thread needs it.
     */
    @Override
    public Socket getClientSocket() {
        return this.clientSocket;
    }

    /**
     * Add basic authentication to request if needed.
     *
     * @param orgRequest  The original request as a byte array.
     * @param proxyToTake The proxy for this Http request.
     * @return The modified request if a basic authentication header is set, else the original request.
     */
    public byte[] addHttpBasicAuthentication(byte[] orgRequest, String proxyToTake) {
        String httpReq = new String(orgRequest, StandardCharsets.US_ASCII);
        String auth = PreProxyFS.getProxyAuthenticationForProxy(proxyToTake);
        byte[] modifiedRequest = orgRequest;
        if (!"".equals(auth) && !httpReq.contains("Proxy-Authorization")) {
            int lengthOfAuth = auth.length();
            int posFirstLineBreak = Util.getFirstLineBreakPos(orgRequest);
            // new byte array with correct length
            modifiedRequest = new byte[orgRequest.length + lengthOfAuth];
            // add first line e.g. "GET http://detectportal.firefox.com/success.txt
            // HTTP/1.1" incl. line break to the modified request
            // A possible string conversion maybe faulty because of the charset?
            System.arraycopy(orgRequest, 0, modifiedRequest, 0, posFirstLineBreak);
            // add proxy authorization: e.g. Proxy-Authorization: Basic
            // YWxhZGRpbjpvcGVuc2VzYW1l in second line
            System.arraycopy(auth.getBytes(StandardCharsets.US_ASCII), 0, modifiedRequest, posFirstLineBreak,
                    auth.getBytes(StandardCharsets.US_ASCII).length);
            // add rest of original request to the modified request
            System.arraycopy(orgRequest, posFirstLineBreak, modifiedRequest,
                    auth.getBytes(StandardCharsets.US_ASCII).length + posFirstLineBreak, 
                    orgRequest.length - posFirstLineBreak);
            Util.traceLogRequestResponse(this.getClass().getName(), orgRequest);
        }
        return modifiedRequest;
    }

    /**
     * Get the correct proxy for this request. It is tested if the proxy is reachable.
     * If the proxy is not reachable, try the DIRECT connection. This is useful, if PreProxyFS
     * is used with a VPN. If the VPN is off, PreProxyFS will forward all requests directly
     * but always with a small delay, because the connection to the proxy is always tested with
     * a timeout.
     *
     * @param httpReq The request converted to a string.
     * @return The proxy to take e.g. remote.proxy1.com:8080
     * @throws ProxyEvaluationException Error while analyzing the PAC script.
     */
    public String getCorrectProxyForHttpRequest(String httpReq) throws ProxyEvaluationException {
        String url = Util.getUrl(httpReq);
        String host = Util.getHost(httpReq);
        String proxyInPAC = PreProxyFS.getPacScriptParser().evaluate(url, host);
        // fallback
        String proxyToTake = "DIRECT";
        String[] proxyInPACSplit = proxyInPAC.split(" ");
        if (proxyInPACSplit.length > 1 && "PROXY".equals(proxyInPACSplit[0])) {
            proxyToTake = proxyInPACSplit[1].split(";")[0];
            // check if proxy is reachable only when timeout is set in configuration
            if (PreProxyFS.getTimeoutForProxyCheck() > 0) {
                proxyToTake = Util.checkIfRemoteProxyIsReachable(proxyToTake);
            } 
        }
        LOGGER.debug("Proxy: {} is used to connect to host: {}", proxyToTake, host);
        return proxyToTake;
    }

    /**
     * Write request to server socket.
     *
     * @param request The request to write.
     * @throws IOException Error while writing to server socket.
     */
    void writeToServerSocket(byte[] request) throws IOException {
        // write/send this request to local socket
        this.distributeForwardServerThread.getServerSocket().getOutputStream().write(request);
        this.distributeForwardServerThread.getServerSocket().getOutputStream().flush();
    }

    /**
     * Start a new forward server thread if the request contains a Http header.
     *
     * @param orgRequest The original request.
     * @return The modified request if basic authentication was added, else original request.
     * @throws IOException              Error while set server socket.
     * @throws ProxyEvaluationException Error getting correct proxy from PAC script.
     */
    public byte[] startForwardServerThreadForHttpRequest(byte[] orgRequest) throws IOException,
            ProxyEvaluationException {
        // convert to string only to check if requests contains CONNECT header
        String httpReq = new String(orgRequest, StandardCharsets.US_ASCII);
        byte[] modifiedRequest = orgRequest;
        LOGGER.debug("Original request: {}", httpReq);
        if (Util.isHttpHeader(httpReq)) {
            // getting url/host from request
            String proxyToTake = getCorrectProxyForHttpRequest(httpReq);
            int localBindPort = PreProxyFS.getLocalProxyPort(proxyToTake);
            // set destination
            this.distributeForwardServerThread.setServerSocket("localhost", localBindPort);
            // start distribute server thread because now we now the destination
            this.distributeForwardServerThread.start();
            // if requests starts with Http header add "Proxy-Authorization: Basic ..." except
            // it already contains authorization
            // get user name and password for the server from configuration / settings file

            modifiedRequest = this.addHttpBasicAuthentication(orgRequest, proxyToTake);
        }
        return modifiedRequest;
    }

    /**
     * Close all sockets.
     */
    void closeSockets() {
        try {
            if (null != this.clientSocket) {
                this.clientSocket.close();
            }
            if (null != this.distributeForwardServerThread
                    && null != this.distributeForwardServerThread.getServerSocket()) {
                this.distributeForwardServerThread.getServerSocket().close();
            }
        } catch (IOException e) {
            LOGGER.trace("Error while closing socket.", e);
        }
    }

    /**
     * Read from client socket and write to the server socket until it is possible.
     * Handle proxy authentication.
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
                request = startForwardServerThreadForHttpRequest(request);
                this.writeToServerSocket(request);
            }
        } catch (IOException e) {
            // Connection is broken --> exit the thread
            LOGGER.trace("Connection is broken.", e);
        } catch (Exception e) {
            LOGGER.debug("An exception occurred during distribute request.", e);
        } finally {
            // Notify parent thread that the connection is broken and forwarding should stop
            this.closeSockets();
        }
    }
}