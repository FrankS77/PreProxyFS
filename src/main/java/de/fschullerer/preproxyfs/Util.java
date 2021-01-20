package de.fschullerer.preproxyfs;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class.
 *
 * @author Frank Schullerer
 */
public final class Util {

    public static final String DIRECT = "DIRECT";
    /**
     * Global default buffer size for connections.
     */
    static final int DEFAULT_BUFFER_SIZE = 65536;

    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class.getName());

    /**
     * Private constructor.
     */
    private Util() {
        // empty
    }

    /**
     * Get the index position after! the first line break (\r\n == 1310) in byte
     * array.
     *
     * @param byteArray The byte array.
     * @return -1 if position is not found else the index position.
     */
    static int getFirstLineBreakPos(byte[] byteArray) {
        int pos = -1;
        for (int index = 0; index < byteArray.length - 3; index++) {
            if (byteArray[index] == 13 && byteArray[index + 1] == 10) {
                pos = index + 2;
                break;
            }
        }
        return pos;
    }

    /**
     * Get URL from request (normally only GET method ?).
     *
     * @param request The request.
     * @return URL or empty string if not found.
     */
    static String getUrl(String request) {
        String url = "";
        String[] req = request.split(" ");
        if (req.length > 2 && req[1].contains(":")) {
            // is URL only if line contains ":" like https:// or ftp: ...
            url = req[1];
        }
        return url;
    }

    /**
     * Get host name from HTTP header.
     *
     * @param request The request.
     * @return The host name e.g. www.google.com. Or unknown.host.com if not found.
     */
    static String getHost(String request) {
        String host = "unknown.host.com";
        boolean found = false;
        Pattern pattern = Pattern.compile("(?m)^Host:\\s+(.+)$");
        Matcher matcher = pattern.matcher(request);
        if (matcher.find()) {
            host = matcher.group(1);
            found = true;
        }
        if (!found) {
            LOGGER.warn("No 'Host:' was found in HTTP request. Must use unknown.host.com");
        }
        return host.split(":")[0];
    }

    /**
     * Check if HTTP requests starts with header.
     *
     * @param request The request.
     * @return TRUE if request starts with HTTP method.
     */
    static boolean isHttpHeader(String request) {
        boolean startsWithHeader = false;
        if ((request.startsWith("GET ") || request.startsWith("POST ") || request.startsWith("PUT ")
                || request.startsWith("HEAD ") || request.startsWith("DELETE ") || request.startsWith("CONNECT ")
                || request.startsWith("OPTIONS ") || request.startsWith("TRACE ") || request.startsWith("PATCH "))
                && request.contains("Host: ")) {
            startsWithHeader = true;
        }
        return startsWithHeader;
    }
    
    /**
     * Get the original request from client socket and reduce the byte length to
     * the real request length (with zeros until end of byte array).
     * So request.length is always the correct length.
     *
     * @param socket The socket to get input stream from.
     * @return The original request.
     * @throws IOException Error getting input stream from client socket.
     */
    public static byte[] readFromClientSocket(Socket socket) throws IOException {
        byte[] newByteArray = new byte[Util.DEFAULT_BUFFER_SIZE];
        byte[] orgRequest;
        // fill new byte array.
        int requestLength = socket.getInputStream().read(newByteArray);
        if (requestLength == -1) {
            // End of stream is reached --> exit the thread
            orgRequest = new byte[] {};
        } else {
            orgRequest = new byte[requestLength];
            System.arraycopy(newByteArray, 0, orgRequest, 0, requestLength);
        }
        return orgRequest;
    }

    static void traceLogRequestResponse(String origin, byte[] buffer) {
        if (LOGGER.isTraceEnabled()) {
            // US_ASCII !!!! not UTF-8 !!
            String httpRes = new String(buffer, StandardCharsets.US_ASCII);
            LOGGER.trace("Logged request/response: {} from: {}", httpRes, origin);
        }
    }
}