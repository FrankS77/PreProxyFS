package de.fschullerer.preproxyfs;

import java.net.Socket;

/**
 * There are different client thread implementations: {@link DirectForwardClientThread}, 
 * {@link DistributeForwardClientThread} and {@link ProxyForwardClientThread} but only
 * one forward thread implementation: {@link ForwardServerThread}. The client thread classes must
 * implement this interface.
 *
 * @author Frank Schullerer
 */
public interface ForwardServerThreadInterface {

    /**
     * Get client socket.
     *
     * @return The client socket.
     */
    Socket getClientSocket();
}