package de.fschullerer.preproxyfs.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


import com.github.markusbernhardt.proxy.selector.pac.JavaxPacScriptParser;
import com.github.markusbernhardt.proxy.selector.pac.ProxyEvaluationException;
import de.fschullerer.preproxyfs.DirectForwardServer;
import de.fschullerer.preproxyfs.DistributeServer;
import de.fschullerer.preproxyfs.PreProxyFS;
import de.fschullerer.preproxyfs.testutil.PacScriptSourceString;
import de.fschullerer.preproxyfs.testutil.ProxyReaderUtilThread;
import de.fschullerer.preproxyfs.testutil.UtilT;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class DistributionServerIT {

    @Mock
    DirectForwardServer mockDirectForwardServer;

    @Test
    @Tag("IntegrationTest")
    @DisplayName("DSIT001: Assert that DistributeServer will correctly forward requests to remote servers.")
    void assertForward1() throws ProxyEvaluationException, IOException {
        // create a helper endpoint-proxy that will read the forwarded request 
        ProxyReaderUtilThread myProxyReader = new ProxyReaderUtilThread();
        myProxyReader.start();
        int proxyReaderPort = myProxyReader.getPort();

        // create DistributeServer for tests
        DistributeServer distributeServer = new DistributeServer(0);
        distributeServer.start();
        
        // this will be a DIRECT connection, mock a DirectForwardServer
        // and set port to the proxy helper thread port
        // DistributeServer will then send DIRECT request to that port
        when(mockDirectForwardServer.getPort()).thenReturn(proxyReaderPort);
        PreProxyFS.setForwardServer(mockDirectForwardServer);
        // a sample PAC script with a DIRECT connection for localhost directions
        PacScriptSourceString pacScript = new PacScriptSourceString(UtilT.PAC_SCRIPT_1);
        PreProxyFS.setPacScriptParser(new JavaxPacScriptParser(pacScript));
        // a sample GET request with the helper proxy port as endpoint
        String originalRequest = "GET / HTTP/1.1\r\nHost: localhost:" + proxyReaderPort + "\r\nUser-agent:foo\r\n";
        // send request to DistributeServer for distribution
        int distributeServerLocalPort = distributeServer.getPort();
        UtilT.simpleWriteToSocket("localhost", distributeServerLocalPort, originalRequest);
        // read the forwarded request from helper proxy
        String requestRead = myProxyReader.getRequest();
        distributeServer.getServerSocket().close();
        assertThat(originalRequest).as("Input and output should be the same!").isEqualTo(requestRead);
    }


}
