package de.fschullerer.preproxyfs.testutil;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class.
 *
 * @author Frank Schullerer
 */
public class UtilT {
    public static final String REMOTE_SERVER_ENDPOINT1 = "corp.example1.com";
    public static final String REMOTE_SERVER_ENDPOINT2 = "corp.example2.com";
    public static final String REMOTE_SERVER_ENDPOINT2_PORT = REMOTE_SERVER_ENDPOINT2 + ":8090";

    public static final String PROXY1_CORPORATE = "proxy1.corporate.com:8080";
    public static final String PROXY2_CORPORATE = "proxy2.corporate.com:8090";

    public static final String[] TEST_AUTH = {"user1", "pass1"};
    public static final String BASIC_AUTH_BASE64_FOR_TEST_AUTH = "dXNlcjE6cGFzczE=";

    public static final String STANDARD_REQUEST_WITH_AUTH = "GET / HTTP/1.1\r\nProxy-Authorization: Basic "
            + BASIC_AUTH_BASE64_FOR_TEST_AUTH + "\r\nHost: " + REMOTE_SERVER_ENDPOINT2_PORT + "\r\nUser-agent:foo\r\n";
    public static final String STANDARD_REQUEST = "GET / HTTP/1.1\r\nHost: " + REMOTE_SERVER_ENDPOINT2_PORT
            + "\r\nUser-agent:foo\r\n";
    public static final String STANDARD_REQUEST_DIRECT_PAC_SCRIPT_V2 =
            "GET / HTTP/1.1\r\nHost: corp.example3.com:8090\r\nUser-agent:foo\r\n";

    /**
     * PAC script with 2 proxies and DIRECT connection as default.
     */
    public static final String PAC_SCRIPT_1 = "function FindProxyForURL(url, host) {\n"
            + "var myIP = myIpAddress ();\n" + "if (host == \"" + REMOTE_SERVER_ENDPOINT1 + "\"){\n" + "return \"PROXY "
            + PROXY1_CORPORATE + "\";}\n" + "if (host == \"" + REMOTE_SERVER_ENDPOINT2 + "\"){\n" + "return \"PROXY "
            + PROXY2_CORPORATE + "\";}\n" + "return \"DIRECT\";}\n";
    
    /**
     * Simple send message to hostname:port.
     *
     * @param hostName The host.
     * @param port     The port.
     * @param toWrite  Message to send.
     * @throws IOException In case writing failed.
     */
    public static void simpleWriteToSocket(String hostName, int port, String toWrite) throws IOException {
        try (Socket socket = new Socket(hostName, port)) {
            socket.getOutputStream().write(toWrite.getBytes());
            socket.getOutputStream().flush();
        }
    }

    /**
     * Create temporary file for tests.
     *
     * @param content Content of file.
     * @return Path to file.
     * @throws IOException If error occurs during creation/writing to file.
     */
    public static String createTempPropFile(String content) throws IOException {
        Path path = Files.createTempFile("testPreProxyFS", ".properties");
        File file = path.toFile();
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        file.deleteOnExit();
        return file.getAbsolutePath();
    }
}
