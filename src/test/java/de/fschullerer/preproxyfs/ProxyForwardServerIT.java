package de.fschullerer.preproxyfs;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.markusbernhardt.proxy.selector.pac.JavaxPacScriptParser;
import com.github.markusbernhardt.proxy.selector.pac.ProxyEvaluationException;
import de.fschullerer.preproxyfs.testutil.PacScriptSourceString;
import de.fschullerer.preproxyfs.testutil.ServerSocketThread;
import de.fschullerer.preproxyfs.testutil.UtilT;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProxyForwardServerIT {

    @Test
    @Tag("IntegrationTest")
    @DisplayName(
            "PFIT001: Assert that ProxyForwardServer will correctly forward requests to remote proxy.")
    void assertForward1() throws ProxyEvaluationException, IOException {
        // create a helper endpoint-proxy that will read the forwarded request to remote proxy
        ServerSocketThread testingServer = new ServerSocketThread();
        testingServer.start();
        int proxyReaderPort = testingServer.getPort();

        // create ProxyForwardServer and set to forward requests to ServerSocketThread
        ProxyForwardServer proxyServer = new ProxyForwardServer("localhost", proxyReaderPort);
        proxyServer.start();

        // a sample PAC script with a DIRECT connection for localhost directions
        PacScriptSourceString pacScript = new PacScriptSourceString(UtilT.PAC_SCRIPT_1);
        PreProxyFS.setPacScriptParser(new JavaxPacScriptParser(pacScript));
        // a sample GET request with the helper proxy port as endpoint
        String originalRequest =
                "GET / HTTP/1.1\r\nHost: localhost:" + proxyReaderPort + "\r\nUser-agent:foo\r\n";
        // send request to proxy server
        int proxyServerLocalPort = proxyServer.getPort();
        UtilT.simpleWriteToSocket("localhost", proxyServerLocalPort, originalRequest);
        // read the forwarded request from helper proxy
        String messageRead = testingServer.getMessagesReceived();
        testingServer.closeSocket();
        proxyServer.getServerSocketP().close();
        assertThat(originalRequest)
                .as("Input and output should be the same!")
                .isEqualTo(messageRead);
    }
}
