package de.fschullerer.preproxyfs;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;

import com.github.markusbernhardt.proxy.selector.pac.JavaxPacScriptParser;
import com.github.markusbernhardt.proxy.selector.pac.ProxyEvaluationException;
import de.fschullerer.preproxyfs.DirectForwardServer;
import de.fschullerer.preproxyfs.DistributeForwardClientThread;
import de.fschullerer.preproxyfs.ForwardServerThread;
import de.fschullerer.preproxyfs.PreProxyFS;
import de.fschullerer.preproxyfs.Util;
import de.fschullerer.preproxyfs.testutil.MockSocket;
import de.fschullerer.preproxyfs.testutil.PacScriptSourceString;
import de.fschullerer.preproxyfs.testutil.UtilT;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test DistributionForwardClientThread.
 *
 * @author Frank Schullerer
 */
@ExtendWith(MockitoExtension.class)
class DistributionForwardClientThreadTest {

    @Mock
    private ForwardServerThread distributeForwardServerThreadMock;

    @Mock
    private DirectForwardServer directForwardServerMock;

    @ParameterizedTest
    @Tag("UnitTest")
    @DisplayName("DS001: Assert that input to client socket is fully read.")
    @ValueSource(strings = {"", UtilT.STANDARD_REQUEST})
    void assertInputReadFromSocketEqualsInput(String request) throws IOException {
        MockSocket sock = new MockSocket();
        sock.setInput(request.getBytes());
        DistributeForwardClientThread ds = new DistributeForwardClientThread(sock);
        byte[] readRequestFromClient = Util.readFromClientSocket(ds.getClientSocket());
        assertThat(request.getBytes()).as("Input and output should be the same!").isEqualTo(readRequestFromClient);
    }

    @Test
    @Tag("UnitTest")
    @DisplayName("DS002: Assert that basic authentication is added if proxy is in proxyAuthenticationMap.")
    void assertBasicAuthIsAdded() {
        Map<String, String[]> proxyAuthenticationMap = new HashMap<>();
        proxyAuthenticationMap.put(UtilT.PROXY1_CORPORATE, UtilT.TEST_AUTH);
        PreProxyFS.setProxyAuthenticationMap(proxyAuthenticationMap);
        MockSocket sock = new MockSocket();
        DistributeForwardClientThread ds = new DistributeForwardClientThread(sock);
        byte[] modifiedRequest =
                ds.addHttpBasicAuthentication(UtilT.STANDARD_REQUEST.getBytes(), UtilT.PROXY1_CORPORATE);
        assertThat(UtilT.STANDARD_REQUEST_WITH_AUTH).as("Proxy-Authorization line should be added to request")
                .isEqualTo(new String(modifiedRequest, StandardCharsets.US_ASCII));
    }

    @Test
    @Tag("UnitTest")
    @DisplayName("DS003: Assert that basic authentication is not added if proxy is not in proxyAuthenticationMap.")
    void assertBasicAuthIsNotAdded() {
        Map<String, String[]> proxyAuthenticationMap = new HashMap<>();
        proxyAuthenticationMap.put(UtilT.PROXY1_CORPORATE, UtilT.TEST_AUTH);
        PreProxyFS.setProxyAuthenticationMap(proxyAuthenticationMap);
        MockSocket sock = new MockSocket();
        DistributeForwardClientThread ds = new DistributeForwardClientThread(sock);
        byte[] notModifiedRequest =
                ds.addHttpBasicAuthentication(UtilT.STANDARD_REQUEST.getBytes(), UtilT.PROXY2_CORPORATE);
        assertThat(UtilT.STANDARD_REQUEST).as("Proxy-Authorization line should not be added to request")
                .isEqualTo(new String(notModifiedRequest, StandardCharsets.US_ASCII));
    }

    @Test
    @Tag("UnitTest")
    @DisplayName("DS004: Assert that basic authentication is not added if there is a proxy authentication in request.")
    void assertBasicAuthIsNotAdded2() {
        Map<String, String[]> proxyAuthenticationMap = new HashMap<>();
        proxyAuthenticationMap.put(UtilT.PROXY1_CORPORATE, UtilT.TEST_AUTH);
        PreProxyFS.setProxyAuthenticationMap(proxyAuthenticationMap);
        MockSocket sock = new MockSocket();
        DistributeForwardClientThread ds = new DistributeForwardClientThread(sock);
        byte[] notModifiedRequest =
                ds.addHttpBasicAuthentication(UtilT.STANDARD_REQUEST_WITH_AUTH.getBytes(), UtilT.PROXY1_CORPORATE);
        assertThat(UtilT.STANDARD_REQUEST_WITH_AUTH).as("Original request should not be modified.")
                .isEqualTo(new String(notModifiedRequest, StandardCharsets.US_ASCII));
    }

    @Test
    @Tag("UnitTest")
    @DisplayName("DS005: Assert that correct proxy is used for request")
    void assertCorrectProxy() throws ProxyEvaluationException {
        MockSocket sock = new MockSocket();
        DistributeForwardClientThread ds = new DistributeForwardClientThread(sock);
        PacScriptSourceString pacScript = new PacScriptSourceString(UtilT.PAC_SCRIPT_1);
        PreProxyFS.setPacScriptParser(new JavaxPacScriptParser(pacScript));
        String proxyToTake = ds.getCorrectProxyForHttpRequest(UtilT.STANDARD_REQUEST);
        assertThat(UtilT.PROXY2_CORPORATE).as("Proxy should be: " + UtilT.PROXY2_CORPORATE)
                .isEqualTo(proxyToTake);
    }

    @Test
    @Tag("UnitTest")
    @DisplayName("DS006: Assert that direct connection is used.")
    void assertDirectConnection() throws ProxyEvaluationException {
        MockSocket sock = new MockSocket();
        DistributeForwardClientThread ds = new DistributeForwardClientThread(sock);
        PacScriptSourceString pacScript = new PacScriptSourceString(UtilT.PAC_SCRIPT_1);
        PreProxyFS.setPacScriptParser(new JavaxPacScriptParser(pacScript));
        String proxyToTake = ds.getCorrectProxyForHttpRequest(UtilT.STANDARD_REQUEST_DIRECT_PAC_SCRIPT_V2);
        assertThat(Util.DIRECT).as("DIRECT connection should be used.")
                .isEqualTo(proxyToTake);
    }

    @Test
    @Tag("UnitTest")
    @DisplayName("DS007: Assert that basic authentication is added for Http request.")
    void testHttpRequestWithStartForwardThread() throws ProxyEvaluationException, IOException {
        Map<String, String[]> proxyAuthenticationMap = new HashMap<>();
        proxyAuthenticationMap.put(UtilT.PROXY2_CORPORATE, UtilT.TEST_AUTH);
        PreProxyFS.setProxyAuthenticationMap(proxyAuthenticationMap);
        PacScriptSourceString pacScript = new PacScriptSourceString(UtilT.PAC_SCRIPT_1);
        PreProxyFS.setPacScriptParser(new JavaxPacScriptParser(pacScript));
        MockSocket sock = new MockSocket();
        DistributeForwardClientThread ds = new DistributeForwardClientThread(sock);
        doNothing().when(distributeForwardServerThreadMock).setServerSocket(isA(String.class), isA(Integer.class));
        doNothing().when(distributeForwardServerThreadMock).start();
        ds.setForwardServerThread(distributeForwardServerThreadMock);
        PreProxyFS.setForwardServer(directForwardServerMock);
        byte[] modifiedRequest = ds.startForwardServerThreadForHttpRequest(UtilT.STANDARD_REQUEST.getBytes());
        assertThat(UtilT.STANDARD_REQUEST_WITH_AUTH).as("Proxy-Authorization line should be added to request")
                .isEqualTo(new String(modifiedRequest, StandardCharsets.US_ASCII));
    }

    @Test
    @Tag("UnitTest")
    @DisplayName("DS007: Assert that basic authentication is added for Http request "
            + "for startForwardServerThreadForHttpRequest.")
    void testNonHttpRequestWithStartForwardThread() throws ProxyEvaluationException, IOException {
        MockSocket sock = new MockSocket();
        DistributeForwardClientThread ds = new DistributeForwardClientThread(sock);
        String originalRequest = "NON_HTTP_METHOD_REQUEST----------------------";
        byte[] nonModifiedRequest = ds.startForwardServerThreadForHttpRequest(originalRequest.getBytes());
        assertThat(originalRequest).as("Non Http method request. Original request should not be modified.")
                .isEqualTo(new String(nonModifiedRequest, StandardCharsets.US_ASCII));
    }

}
