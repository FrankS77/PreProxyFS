package de.fschullerer.preproxyfs;

import static org.assertj.core.api.Assertions.assertThat;

import de.fschullerer.preproxyfs.testutil.ClientSocketThread;
import de.fschullerer.preproxyfs.testutil.ServerSocketThread;
import de.fschullerer.preproxyfs.testutil.UtilT;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Full integration test of PreProxyFS. */
class PreProxyFSIT {
    static ServerSocketThread testingProxyServer1;
    static ServerSocketThread testingProxyServer2;
    static ServerSocketThread directConnectionEndpoint;
    static int directConnectionEndpointPort;

    @BeforeAll
    public static void setup() throws IOException {
        // first proxy server in PAC script
        testingProxyServer1 = new ServerSocketThread();
        testingProxyServer1.start();
        // second proxy server in PAC script
        testingProxyServer2 = new ServerSocketThread();
        testingProxyServer2.start();
        // DIRECT connection endpoint
        directConnectionEndpoint = new ServerSocketThread();
        directConnectionEndpoint.start();
        directConnectionEndpointPort = directConnectionEndpoint.getPort();
        int proxyReaderPort1 = testingProxyServer1.getPort();
        int proxyReaderPort2 = testingProxyServer2.getPort();
        String pac2 =
                "function FindProxyForURL(url, host) {if (host == \""
                        + UtilT.REMOTE_SERVER_ENDPOINT1
                        + "\"){"
                        + "return \"PROXY localhost:"
                        + proxyReaderPort1
                        + ";\"}"
                        + "if (host == \""
                        + UtilT.REMOTE_SERVER_ENDPOINT2
                        + "\"){ return \"PROXY localhost:"
                        + proxyReaderPort2
                        + ";\"} return \"DIRECT\";}";
        String pathToPACScript = UtilT.createTempPropFile(pac2);
        PreProxyFS.startPreProxyFS(pathToPACScript, PreProxyFS.DEFAULT_LOCAL_BIND_PORT, null);
    }

    // @Test
    @Tag("IntegrationTest")
    @DisplayName("PREFSIT001: Assert that request to main port is forwarded to 1. proxy.")
    void assertPreFSIT1() throws IOException {
        String proxy1Req =
                "GET / HTTP/1.1\r\nHost: "
                        + UtilT.REMOTE_SERVER_ENDPOINT1
                        + "\r\nUser-agent:foo\r\n";
        UtilT.simpleWriteToSocket("localhost", PreProxyFS.DEFAULT_LOCAL_BIND_PORT, proxy1Req);
        String proxy1Received = testingProxyServer1.getMessagesReceived();
        assertThat(proxy1Req).as("Input and output should be the same!").isEqualTo(proxy1Received);
    }

    // @Test
    @Tag("IntegrationTest")
    @DisplayName("PREFSIT002: Assert that request to main port is forwarded to 2. proxy.")
    void assertPreFSIT2() throws IOException {
        String proxy2Req =
                "GET / HTTP/1.1\r\nHost: "
                        + UtilT.REMOTE_SERVER_ENDPOINT2
                        + "\r\nUser-agent:foo\r\n";
        UtilT.simpleWriteToSocket("localhost", PreProxyFS.DEFAULT_LOCAL_BIND_PORT, proxy2Req);
        String proxy2Received = testingProxyServer2.getMessagesReceived();
        assertThat(proxy2Req).as("Input and output should be the same!").isEqualTo(proxy2Received);
    }

    @Test
    @Tag("IntegrationTest")
    @DisplayName("PREFSIT003: Assert that request to main port is forwarded directly.")
    void assertPreFSIT3() throws IOException {
        String connectRequest =
                "CONNECT localhost:"
                        + directConnectionEndpointPort
                        + " HTTP/1.1\r\nHost: localhost:"
                        + directConnectionEndpointPort
                        + "\r\n";
        String directReq =
                "GET http://localhost:"
                        + directConnectionEndpointPort
                        + " HTTP/1.1\r\nHost: localhost\r\nUser-agent:foo\r\n";
        ClientSocketThread testingClient =
                new ClientSocketThread("localhost", PreProxyFS.DEFAULT_LOCAL_BIND_PORT);
        testingClient.start();
        testingClient.writeToSocket(connectRequest);
        // first message should be Util.CONNECTION_ESTABLISHED but is checked in test before
        testingClient.resetMessages();
        testingClient.writeToSocket(directReq);
        // the GET request should be received by testing server
        String directReceived = directConnectionEndpoint.getMessagesReceived();
        testingClient.closeSocket();
        assertThat(directReq).as("Input and output should be the same!").isEqualTo(directReceived);
    }

    @AfterAll
    public static void tearDown() {
        PreProxyFS.stopPreProxyFS();
        testingProxyServer1.closeSocket();
        testingProxyServer2.closeSocket();
        directConnectionEndpoint.closeSocket();
    }
}
