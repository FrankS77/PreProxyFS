package de.fschullerer.preproxyfs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.fschullerer.preproxyfs.PreProxyFS;
import de.fschullerer.preproxyfs.PreProxyFSException;
import de.fschullerer.preproxyfs.Util;
import de.fschullerer.preproxyfs.testutil.ServerSocketThread;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit test Util class.
 */
class UtilTest {

    @Test
    @Tag("UnitTest")
    @DisplayName("Util001: Assert that the proxy connection is used if proxy is available.")
    void assertUtil1() {
        ServerSocketThread testingProxyServer1 = new ServerSocketThread();
        testingProxyServer1.start();
        String proxyThatIsAlive = "localhost:" + testingProxyServer1.getPort();
        String actualProxy = Util.checkIfRemoteProxyIsReachable(proxyThatIsAlive);
        testingProxyServer1.closeSocket();
        assertThat(proxyThatIsAlive).as("Input and output should be the same!").isEqualTo(actualProxy);
    }

    @Test
    @Tag("UnitTest")
    @DisplayName("Util002: Assert that an DIRECT connection is used if proxy is not available.")
    void assertUtil2() {
        String proxyThatIsAlive = "localhost:0";
        String direct = Util.checkIfRemoteProxyIsReachable(proxyThatIsAlive);
        assertThat(Util.DIRECT).as("If proxy is not available take DIRECT connection!").isEqualTo(direct);
    }
}
