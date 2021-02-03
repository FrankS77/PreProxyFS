package de.fschullerer.preproxyfs;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.markusbernhardt.proxy.selector.pac.JavaxPacScriptParser;
import com.github.markusbernhardt.proxy.selector.pac.ProxyEvaluationException;
import de.fschullerer.preproxyfs.DirectForwardServer;
import de.fschullerer.preproxyfs.PreProxyFS;
import de.fschullerer.preproxyfs.Util;
import de.fschullerer.preproxyfs.testutil.ClientSocketThread;
import de.fschullerer.preproxyfs.testutil.PacScriptSourceString;
import de.fschullerer.preproxyfs.testutil.ServerSocketThread;
import de.fschullerer.preproxyfs.testutil.UtilT;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * DirectForwardServer integration tests.
 */
class DirectForwardServerIT {
    
    @Test
    @Tag("IntegrationTest")
    @DisplayName("DFIT001: Assert that a Connection established is received when sending a connect request to the " 
                         + "DirectForwardServer.")
    void assertForward1() throws ProxyEvaluationException, IOException {
        // create DistributeServer for tests
        DirectForwardServer directForwardServer = new DirectForwardServer();
        directForwardServer.start();
        // a sample PAC script with a DIRECT connection for localhost directions
        PacScriptSourceString pacScript = new PacScriptSourceString(UtilT.PAC_SCRIPT_1);
        PreProxyFS.setPacScriptParser(new JavaxPacScriptParser(pacScript));
        // a sample CONNECT request
        String connectRequest = "CONNECT remote.server.com:8080 HTTP/1.1\r\nHost: remote.server.com:8080\r\n";
        int directServerPort = directForwardServer.getPort();
        // create a testing client which connects to the DirectForwardServer and can send and receive messsages
        ClientSocketThread testingClient = new ClientSocketThread("localhost", directServerPort);
        testingClient.start();
        testingClient.writeToSocket(connectRequest);
        // read messages received from DirectForwardClientThread
        String requestRead = testingClient.getMessagesReceived();
        testingClient.closeSocket();
        assertThat(Util.CONNECTION_ESTABLISHED).as("A Connection established should be received " 
                                                           + "but that is not the case.").isEqualTo(requestRead);
    }

    @Test
    @Tag("IntegrationTest")
    @DisplayName("DFIT002: Assert that a connection is established and.")
    void assertForward2() throws ProxyEvaluationException, IOException {
        // create DistributeServer for tests
        DirectForwardServer directForwardServer = new DirectForwardServer();
        directForwardServer.start();
        // a sample PAC script with a DIRECT connection for localhost directions
        PacScriptSourceString pacScript = new PacScriptSourceString(UtilT.PAC_SCRIPT_1);
        PreProxyFS.setPacScriptParser(new JavaxPacScriptParser(pacScript));
        // create a helper endpoint-proxy that will read the forwarded request 
        ServerSocketThread testingServer = new ServerSocketThread();
        testingServer.start();
        int testingServerPort = testingServer.getPort();
        // a CONNECT request to the testing ServerSocketThread
        String connectRequest = "CONNECT localhost:" + testingServerPort + " HTTP/1.1\r\nHost: localhost:" 
                                         + testingServerPort + "\r\n";
        int directServerPort = directForwardServer.getPort();
        // create a testing client which connects to the DirectForwardServer and can send and receive messsages
        ClientSocketThread testingClient = new ClientSocketThread("localhost", directServerPort);
        testingClient.start();
        testingClient.writeToSocket(connectRequest);
        // first message should be Util.CONNECTION_ESTABLISHED but is checked in test before
        testingClient.resetMessages();
        String getRequest = "GET / localhost:" + testingServerPort + " HTTP/1.1\r\nHost: localhost:"
                                        + testingServerPort + "\r\n";
        testingClient.writeToSocket(getRequest);
        // the GET request should be received by testing server
        String responseFromTestingServer = testingServer.getMessagesReceived();
        testingClient.closeSocket();
        assertThat(getRequest).as("Input and output should be the same!").isEqualTo(responseFromTestingServer);
    }
}
