package de.fschullerer.preproxyfs.testutil;

import de.fschullerer.preproxyfs.PreProxyFSException;
import de.fschullerer.preproxyfs.Util;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For testing purposes: Create a client socket connection to a remote or local host and
 * send/receive messages.
 *
 * @author Frank Schullerer
 */
public class ClientSocketThread extends Thread {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClientSocketThread.class.getName());

    private final Object waitForRequestReadFinished = new Object();
    private boolean acceptRequests = true;
    private StringBuilder messageStorage = new StringBuilder();
    private Socket clientSocket;

    /**
     * Create a connection to a host.
     *
     * @param hostName The hostname e.g. localhost.
     * @param port The port.
     * @throws IOException If an error occurred while creating connection.
     */
    public ClientSocketThread(String hostName, int port) throws IOException {
        clientSocket = new Socket(hostName, port);
    }

    /**
     * Get the messages that was received by this socket.
     *
     * @return The messages.
     */
    public String getMessagesReceived() {
        // this thread waits until notified in run method
        waitForRequestToBeRead();
        String message = messageStorage.toString();
        // clean for new messages
        messageStorage = new StringBuilder();
        return message;
    }

    /**
     * Alternative for {@link #getMessagesReceived} if messages before are not interesting. Wait
     * until request is read.
     */
    public void resetMessages() {
        waitForRequestToBeRead();
        messageStorage = new StringBuilder();
    }

    /** Close client socket. */
    public void closeSocket() {
        this.acceptRequests = false;
        if (null != clientSocket) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                LOGGER.trace("Error closing test socket", e);
            }
        }
    }

    /**
     * Write to the remote socket (send message).
     *
     * @param toWrite The message to send.
     * @throws IOException If an error occurs while sending message.
     */
    public void writeToSocket(String toWrite) throws IOException {
        this.clientSocket.getOutputStream().write(toWrite.getBytes());
        this.clientSocket.getOutputStream().flush();
    }

    private void waitForRequestToBeRead() {
        synchronized (waitForRequestReadFinished) {
            try {
                waitForRequestReadFinished.wait(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PreProxyFSException("Timeout waiting for request string to be read. ", e);
            }
        }
    }

    /** Read messages from this socket and store them into {@link #messageStorage}. */
    @Override
    public void run() {
        while (acceptRequests) {
            byte[] message;
            try {
                message = Util.readFromClientSocket(this.clientSocket);
                if (message.length != 0) {
                    String messageString = new String(message, StandardCharsets.US_ASCII);
                    messageStorage.append(messageString);
                    System.out.println("messageString " + messageString);
                    // notify if message received
                    synchronized (waitForRequestReadFinished) {
                        waitForRequestReadFinished.notifyAll();
                    }
                }
            } catch (Exception e) {
                LOGGER.trace("Only for debugging tests", e);
            }
        }
    }
}
