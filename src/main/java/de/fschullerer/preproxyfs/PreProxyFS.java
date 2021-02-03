package de.fschullerer.preproxyfs;

import com.github.markusbernhardt.proxy.selector.pac.JavaxPacScriptParser;
import com.github.markusbernhardt.proxy.selector.pac.PacScriptParser;
import com.github.markusbernhardt.proxy.selector.pac.UrlPacScriptSource;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for PreProxyFS.
 *
 * <p>PreProxyFS is designed to solve the following problem: For example, one is in a corporate network and
 * needs to use a PAC script (https://en.wikipedia.org/wiki/Proxy_auto-config) to reach certain Internet
 * or intranet addresses but the program that needs this  access can only use a normal proxy on one port.
 * In addition, to use one or more defined proxies, a username and password must be entered
 * (Basic Authentication). This is also not supported by several programs (unlike Internet browsers).
 *
 * <p>PreProxyFS can now read a defined PAC script and only provide this service on a local port.
 * Requests on this one local port are distributed according to the rules of the PAC file to the
 * proxies defined in it and if necessary provided with the Basic Authentication header,
 * so that no user name and password input are necessary any more.
 *
 * @author Frank Schullerer
 */
public class PreProxyFS {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreProxyFS.class.getName());

    public static final int DEFAULT_LOCAL_BIND_PORT = 65000;
    /**
     * The list contain remote proxies that were started by
     * {@link #startProxyForwardServer}.
     */
    static final CopyOnWriteArrayList<ProxyForwardServer> proxyForwardServerList
            = new CopyOnWriteArrayList<>();
    /**
     * With {@link PacScriptParser#evaluate} can we check which proxy to use for a
     * specific Intranet/Internet address.
     */
    private static PacScriptParser pacScriptParser;
    private static int mainPort;
    private static String pacUrl;
    private static DistributeServer mainDistributionServer;
    private static DirectForwardServer directForwardServer;

    private static int timeoutForProxyCheck;
    
    /**
     * The stored/defined proxy authentications from settings file.
     */
    private static Map<String, String[]> proxyAuthenticationMap = new HashMap<>();

    public static void setProxyAuthenticationMap(Map<String, String[]> authMap) {
        proxyAuthenticationMap = authMap;
    }
    
    /**
     * Get the timeout to wait for connecting to the remote proxies.
     *
     * @return 0 if there should be no check.
     */
    public static int getTimeoutForProxyCheck() {
        return timeoutForProxyCheck;
    }
    
    /**
     * Base64 convert user name and password for proxy authentication.
     *
     * @param user User name for proxy.
     * @param pass Password for proxy.
     * @return The full Proxy-Authorization HTTP request header line. Ready to insert it to request header.
     */
    private static String getProxyAuth(String user, String pass) {
        String userPass = user + ":" + pass;
        // read from settings file with utf-8
        return "Proxy-Authorization: Basic " + Base64.getEncoder()
            .encodeToString(userPass.getBytes(StandardCharsets.UTF_8))  + "\r\n";
    }

    /**
     * Get the stored/defined proxy authentication (from settings file) for a
     * special proxy.
     *
     * @param proxy The proxy as proxyDNS:port string e.g. my.remote.proxy.com:8080
     * @return The Base64 converted user name and password for proxy authentication e.g.
     *         Proxy-Authorization: Basic YWxhZGRpbjpvcGVuc2VzYW1l (with line break at the end \r\n)
     */
    static String getProxyAuthenticationForProxy(String proxy) {
        String[] userPass = null;
        String userPassEncoded = "";
        if (!"".equals(proxy)) {
            for (Map.Entry<String, String[]> entry : proxyAuthenticationMap.entrySet()) {
                if (proxy.equals(entry.getKey())) {
                    userPass = entry.getValue();
                    break;
                }
            }
            if (null != userPass) {
                userPassEncoded = getProxyAuth(userPass[0], userPass[1]);
            }
        }
        return userPassEncoded;
    }
    
    /**
     * Read settings from properties file.
     *
     * @param settingsFilePath Path to settings file.
     */
    private static void readSettings(String settingsFilePath) {
        // Read properties file in a Property object
        Properties props = new Properties();
        // read from settings file with utf-8
        try (FileInputStream settingsFile = new FileInputStream(settingsFilePath);
                InputStreamReader in = new InputStreamReader(settingsFile, StandardCharsets.UTF_8)) {
            props.load(in);
        } catch (Exception e) {
            throw new PreProxyFSException("Failure loading the settings file: " + settingsFilePath + " Program exit.",
                    e);
        }
        setPacUrl(props.getProperty("PAC_URL"));
        setMainBindPort(props.getProperty("MAIN_LOCAL_PORT"));
        proxyAuthenticationMap = setUserPasswordMap(props.getProperty("USER_PASSWORD_MAP", ""));
        setTimeoutForProxyCheck(props.getProperty("TIMEOUT_FOR_PROXY_CHECK", "0"));
    }

    /**
     * Setting (mandatory): Set PAC URL/file.
     *
     * @param pacFileOrUrl e.g. http://my.proxy.domain.com/server.pac or c:/myPac.pac
     */
    private static void setPacUrl(String pacFileOrUrl) {
        if (null == pacFileOrUrl || "".equals(pacFileOrUrl)) {
            throw new PreProxyFSException("No PAC file/URL given in settings. Program exit.");
        }
        pacUrl = pacFileOrUrl;
        LOGGER.info("Using pac script: {}", pacUrl);
    }

    /**
     * Setting (optional): Set {@link #mainPort}.
     *
     * @param bindPort The local server socket port number to set.
     */
    private static void setMainBindPort(String bindPort) {
        if (bindPort == null) {
            LOGGER.info(
                    "MAIN_LOCAL_PORT is not defined in properties file. Using default port: "
                            + DEFAULT_LOCAL_BIND_PORT);
            mainPort = DEFAULT_LOCAL_BIND_PORT;
        } else {
            try {
                mainPort = Integer.parseInt(bindPort);
            } catch (NumberFormatException e) {
                LOGGER.warn(
                        "MAIN_LOCAL_PORT in properties file is not a number. Using default port: {}",
                        DEFAULT_LOCAL_BIND_PORT);
                mainPort = DEFAULT_LOCAL_BIND_PORT;
            }
        }
    }
    
    private static void setTimeoutForProxyCheck(String timeout) {
        // 0 means: do not use this feature
        int timeoutToSet = 0;
        if (Util.isNumeric(timeout)) {
            timeoutToSet = Integer.parseInt(timeout);
        }
        timeoutForProxyCheck = timeoutToSet;
    }

    /**
     * Setting (optional): Get the proxy username password combinations from
     * USER_PASSWORD_MAP settings file and set {@link #proxyAuthenticationMap}.
     *
     * @param usersAndPassForProxies The USER_PASSWORD_MAP property value. Can be
     *                               empty.
     * @return Map with user authentications for the PAC proxies.
     */
     static Map<String, String[]> setUserPasswordMap(String usersAndPassForProxies) { 
        Map<String, String[]> userAuth = new HashMap<>();
        if (!"".equals(usersAndPassForProxies)) {
            int leftSquareBracketCount = count(usersAndPassForProxies, "[");
            int rightSquareBracketCount = count(usersAndPassForProxies, "]");
            if ((leftSquareBracketCount < 4) || leftSquareBracketCount != rightSquareBracketCount) {
                throw new PreProxyFSException("The configuration for USER_PASSWORD_MAP is wrong. Tip: Count "
                   + "the square brackets [ and ] in variable. It must be 4 for each entry. Program exit.");
            }
            List<String> tempList = new ArrayList<>();
            int indexToCut;
            String subProxyAuthListString = usersAndPassForProxies;
            while (!"".equals(subProxyAuthListString)) {
                indexToCut = findIndexOfLeftBracket(subProxyAuthListString) + 1;
                tempList.add(subProxyAuthListString.substring(0, indexToCut));
                subProxyAuthListString = subProxyAuthListString.substring(indexToCut);
            }
            for (String entry : tempList) {
                String[] subEntries = entry.split("\\[");
                String remoteProxy = subEntries[1].replace("]", "").trim();
                String username = subEntries[3].replace("]", "").trim();
                String password = subEntries[4].replace("]", "").trim();
                // if there must be a [ or ] is password, use the HTML code one
                password = password.replace("&#91;", "[");
                password = password.replace("&#93;", "]");
                userAuth.put(remoteProxy, new String[] {username, password});
            }
        }
        return userAuth;
    }

    /**
     * Count substring occurrences in text.
     *
     * @param text      Text to check.
     * @param substring String to find in text.
     * @return -1 if no occurrences else the number of occurrences.
     */
    private static int count(String text, String substring) {
        if ("".equals(substring)) {
            return -1;
        }
        return (text.length() - text.replace(substring, "").length()) / substring.length();
    }

    private static void addProxyToList(String hostName, String proxyPort) {
        ProxyForwardServer remoteProxyForward = new ProxyForwardServer(hostName,
                Integer.parseInt(proxyPort));
        proxyForwardServerList.add(remoteProxyForward);
    }

    /**
     * Start a {@link DirectForwardServer} for every proxy in the PAC script. Every
     * {@link DirectForwardServer} will create a local socket to communicate with the
     * proxy.
     *
     * @param pacSource PAC script content.
     */
    private static void startProxyForwardServer(String pacSource) {
        Pattern pattern = Pattern.compile("PROXY\\s+([^'\":;\\s]+):(\\d+)");
        Matcher matcher = pattern.matcher(pacSource);
        String proxy;
        boolean atLeastOneProxyFound = false;
        // We need more local ports for the sockets to the remote proxies defined in the PAC
        // Use random free ports but put them into the list
        while (matcher.find()) {
            proxy = matcher.group(1) + ":" + matcher.group(2);
            atLeastOneProxyFound = true;
            if (proxyForwardServerList.isEmpty()) {
                addProxyToList(matcher.group(1), matcher.group(2));
            } else {
                for (ProxyForwardServer proxyInList : proxyForwardServerList) {
                    if (!proxyInList.getProxy().equals(proxy)) {
                        addProxyToList(matcher.group(1), matcher.group(2));
                        break;
                    }
                }
            }
        }
        // start proxies threads
        for (ProxyForwardServer proxyInList : proxyForwardServerList) {
            proxyInList.start();
        }

        if (!atLeastOneProxyFound) {
            LOGGER.warn("Not even one proxy was found in the Pac Script.");
        }
        // create one DIRECT forward server for direct connections
        if (null == directForwardServer) {
            setForwardServer(new DirectForwardServer());
        }
        directForwardServer.start();
    }

    /**
     * (Separate call for better mock testing).
     *
     * @param directServer DirectForwardServer
     */
    public static void setForwardServer(DirectForwardServer directServer) {
        directForwardServer = directServer;
    }

    /**
     * Get the local proxy port number.
     *
     * @param proxyName Remote proxy name or "DIRECT"
     * @return Local port number.
     */
    static int getLocalProxyPort(String proxyName) {
        int portNumber = 0;
        if ("DIRECT".equals(proxyName)) {
            portNumber = directForwardServer.getPort();
        } else {
            for (ProxyForwardServer proxyInList : proxyForwardServerList) {
                if (proxyInList.getProxy().equals(proxyName)) {
                    portNumber = proxyInList.getPort();
                    break;
                }
            }
        }
        if (portNumber == 0) {
            LOGGER.warn("Should not happen: No proxy found!");
        }
        return portNumber;
    }

    /**
     * Get version number from MANIFEST.MF
     *
     * @return Version number.
     */
    private static String getVersionNumber() {
        return PreProxyFS.class.getPackage().getImplementationVersion();
    }

    /**
     * Start PreProxyFS with this method, if you want to start PreProxyFS from within
     * another JAVA program.
     *
     * @param pacFilePathOrUrl Mandatory: URL or file path to the settings properties file.
     *                         e.g. http://remote.proxy.pac.server.com/proxy.pac  or file:/c:/myProxy.pac
     * @param localPortBind    Optional (can be null, default value is than used).
     *                         The local port to bind to. This local port and the following ports
     *                         (localPortBind +1 +2 +3 ...) must not be in use! Better use a higher port.
     *                         Default is 65000.
     *                         If a lower port than 80 is set, the default value is used.
     * @param proxyAuth        Optional (can be null). If one or more proxies in the defined PAC script
     *                         use basic authentication, put a new key/value pair to this map
     *                         e.g. the.first.proxy.in.pac.script.com:8080,
     *                         new String[]{"usernameForProxy","passwordForProxy"}.
     *                         Attention: If there is a square bracket [ or ] in your password,
     *                         please use &#91; for left square bracket
     *                         [  and use  &#93; for right square bracket
     */
    public static void startPreProxyFS(String pacFilePathOrUrl, Integer localPortBind,
                                       Map<String, String[]> proxyAuth) {
        try {
            LOGGER.info("----------------------------------------");
            LOGGER.info("");
            LOGGER.info("Starting PreProxyFS!");
            LOGGER.info("");
            LOGGER.info("Author: Frank Schullerer");
            LOGGER.info("Version: {}", getVersionNumber());
            LOGGER.info("");
            LOGGER.info("----------------------------------------");
            setPacUrl(pacFilePathOrUrl);
            if (null != localPortBind) {
                mainPort = localPortBind;
            } else {
                mainPort = DEFAULT_LOCAL_BIND_PORT;
            }
            if (null != proxyAuth) {
                proxyAuthenticationMap = proxyAuth;
            }
            UrlPacScriptSource pacScript = new UrlPacScriptSource(pacUrl);
            String pacScriptContent = pacScript.getScriptContent();
            LOGGER.debug("Pac script content: {}", pacScriptContent);
            // create sockets for every remote proxy in PAC
            startProxyForwardServer(pacScriptContent);
            setPacScriptParser(new JavaxPacScriptParser(pacScript));
            LOGGER.info("Initial pac script parsing finished.");
            if (null == mainDistributionServer) {
                // if clause for better testing
                setDistributeServer(new DistributeServer(mainPort));
            }
            // start main distribution thread that distributes requests to this local port
            // to the remote proxies started by {@link #startProxyForwardServer}
            mainDistributionServer.start();
        } catch (Exception e) {
            throw new PreProxyFSException("A fatal error occurred during startup. Program exit.", e);
        }
    }

    /**
     * (Separate call for better mock testing).
     *
     * @param distributeServer DistributeServer
     */
    static void setDistributeServer(DistributeServer distributeServer) {
        mainDistributionServer = distributeServer;
    }

    /**
     * Stop PreProxyFS. Use it from within other Java programs.
     */
    @SuppressWarnings("unused")
    public static void stopPreProxyFS() {
        try {
            for (ProxyForwardServer proxyInList : proxyForwardServerList) {
                LOGGER.info("Try to stop proxy thread for port: {}", proxyInList.getPort());
                if (null != proxyInList.getServerSocketP()) {
                    proxyInList.getServerSocketP().close();
                }
            }
            if (null != directForwardServer && null != directForwardServer.getServerSocketD()) {
                LOGGER.info("Try to stop direct connection thread for port: {}", directForwardServer.getPort());
                directForwardServer.getServerSocketD().close();
            }
            if (null != mainDistributionServer && null != mainDistributionServer.getServerSocket()) {
                LOGGER.info("Try to stop main distribution server thread for port: {}", mainPort);
                mainDistributionServer.getServerSocket().close();
            }
        } catch (Exception e) {
            LOGGER.debug("Errors during closing threads.", e);
        }
    }

    private static int findIndexOfLeftBracket(String text) {
        int nthOccurrence = 4;
        String substring = "]";
        int index = text.indexOf(substring);
        while (--nthOccurrence > 0 && index != -1) {
            index = text.indexOf(substring, index + 1);
        }
        return index;
    }

    /**
     * Main program entry point. Reads settings and start {@link #startPreProxyFS}
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            LOGGER.error("PreProxyFS needs as parameter the full path to the PreProxyFS.properties file!");
            LOGGER.error("Call PreProxyFS script like e.g. (Linux) sh PreProxyFS /path/to/PreProxyFS.properties ");
            LOGGER.error("for e.g. (Windows) PreProxyFS.bat c:/path/to/PreProxyFS.properties ");
        } else {
            // read settings file
            readSettings(args[0]);
            startPreProxyFS(pacUrl, mainPort, proxyAuthenticationMap);
        }
    }

    public static PacScriptParser getPacScriptParser() {
        return pacScriptParser;
    }

    public static void setPacScriptParser(PacScriptParser pacScriptParser) {
        PreProxyFS.pacScriptParser = pacScriptParser;
    }
}