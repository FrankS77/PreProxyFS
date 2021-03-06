# PreProxyFS
A pre-proxy with PAC and basic authentication support for users in corporate networks.

PreProxyFS is designed to solve the following problem: For example, someone is in a corporate network and 
needs to use a PAC script (https://en.wikipedia.org/wiki/Proxy_auto-config) to reach certain Internet 
or Intranet addresses, but the program that needs this access can only use a normal proxy on one port. 
Sometimes also a username and password must be entered (Basic Authentication). 
This is also not supported by several programs (unlike Internet browsers).
 
PreProxyFS can read a defined PAC script (file or via http) and provide this service on one local port.  
Requests on this local port are distributed according to the rules of the PAC script to the 
proxies defined in it and if necessary provided with the Basic Authentication header, 
so that no username and password input are necessary anymore. 

## Based on
PreProxyFS is based on the great work of proxy-vole (https://github.com/MarkusBernhardt/proxy-vole),
delight-rhino-sandbox (https://github.com/javadelight/delight-rhino-sandbox) and mozilla-rhino
(https://github.com/mozilla/rhino).

## Features
* A remote or local PAC script can be used
* You only have one local port (default 65000) for all the proxies defined in the PAC script
* You can define basic authentication for multiple proxies
* PreProxyFS can be used standalone or as a dependency in other Java programs

## Limitations
* Only http(s) proxies in PAC are supported (SOCKS proxies are not supported) 

### Usage as a standalone Java program (see below for native executables)
* Precondition: Minimum installed Java JRE 8
* Download and extract the PreProxyFS zip file PreProxyFS-Jar-*.zip
  (the folder should contain the PreProxyFS<Version>.jar file and the lib folder). 
* Create a (or more) PreProxyFS.properties file. Example:

```properties
# Mandatory: Local proxy port. Must be a free local post. Default 65000
MAIN_LOCAL_PORT = 65000
# Mandatory: Path or URL to PAC script e.g. http://my.pac.server/remote.pac or /home/myuser/mypac.pac (windows c:/mypac.pac)
PAC_URL = http://my.pac.server/remote.pac
# Optional: Password basic authentication for a proxy/proxies in PAC script [<proxyDNSname>:<ProxyPort>[[<myUserName>][<myPassword>]]]
#           Attention: If there is a square bracket [ or ] in your password, please use &#91; for left square bracket [  and use  &#93; for right square bracket
USER_PASSWORD_MAP = [remote.proxy1.com:8080[[myUserName][myPassword]]][remote.proxy2.com:8080[[myUserName2][myPassword2]]]
# Optional: Check if the remote proxies (from PAC script) are reachable before forwarding the request.
# This can be useful, if e.g. PreProxyFS is used with a VPN. If the VPN is off, PreProxyFS will forward all requests directly
# and not via the proxies (which are not reachable if the VPN is off. 
# Disadvantage is a a small delay, because the connection to the proxy is always tested with a timeout. This timeout
# in milliseconds can be set. Default is 0. This means there should be no check and the connection will fail if a 
# remote proxy is not reachable. If you set the the value too low e.g. < 20 (milliseconds) it can be that a DIRECT
# connection is used even if the proxy is reachable but maybe your internet connection latency is a little bit too high.
# If you set the timeout too high (e.g. 1000) you will have a noticeable delay while surfing the internet.
TIMEOUT_FOR_PROXY_CHECK = 0
```
* Put the property file somewhere in your filesystem e.g. /home/myuser/PreProxyFS.properties
* Start PreProxyFs on command line with: java -jar PreProxyFS*.jar /home/myuser/PreProxyFS.properties
* Output should be something like:

```
[main] INFO de.fschullerer.preproxyfs.PreProxyFS - Using pac script: /Users/user/myPac.pac
[main] INFO de.fschullerer.preproxyfs.PreProxyFS - ----------------------------------------
[main] INFO de.fschullerer.preproxyfs.PreProxyFS - 
[main] INFO de.fschullerer.preproxyfs.PreProxyFS - Starting PreProxyFS!
[main] INFO de.fschullerer.preproxyfs.PreProxyFS - 
[main] INFO de.fschullerer.preproxyfs.PreProxyFS - Author: Frank Schullerer
[main] INFO de.fschullerer.preproxyfs.PreProxyFS - 
[main] INFO de.fschullerer.preproxyfs.PreProxyFS - ----------------------------------------
[main] INFO de.fschullerer.preproxyfs.PreProxyFS - Using pac script: /Users/user/myPac.pac
[Thread-0] INFO de.fschullerer.preproxyfs.ProxyForwardServer - Start ProxyForwardServer on TCP port: 63725 . Connected to remote proxy: remote.proxy1.com:8000
[Thread-1] INFO de.fschullerer.preproxyfs.ProxyForwardServer - Start ProxyForwardServer on TCP port: 63727 . Connected to remote proxy: remote.proxy2.com:8081
[Thread-2] INFO de.fschullerer.preproxyfs.ProxyForwardServer - Start ProxyForwardServer on TCP port: 63730 . Connected to remote proxy: remote.proxy3.com:8080
[Thread-3] INFO de.fschullerer.preproxyfs.DirectForwardServer - Start DirectForwardServer on TCP port : 63733
[main] INFO de.fschullerer.preproxyfs.PreProxyFS - Initial pac script parsing finished.
[Thread-4] INFO de.fschullerer.preproxyfs.DistributeServer - Start DistributeServer on TCP port: 65000
```
* Go to your program (e.g. Internet browser) and set the http proxy setting to:  localhost:65000 (or the port you have defined) and start browsing!

### Usage within another Java program
* PreProxyFS is not yet downloadable via "Maven Central" etc. maybe in the future so:
* Download the Jar from GitHub and put it locally to your program dependencies

```
import de.fschullerer.preproxyfs.PreProxyFS;

// Mandatory: path or URL to PAC script
String pacFilePathOrUrl = "/path/to/my/may.pac" 
// or pacFilePathOrUrl = "http://my.company.com/company.pac" 

// Mandatory: Set local port (must be a free port)
int localPortBind = 65000;

// Optional (can be null): Basic authentication for one or more proxies in PAC script
Map<String, String[]> proxyAuthenticationMap = new HashMap<>();
proxyAuthenticationMap.put("remote.proxy1.com:8000", new String[]{"proxyusername","proxyPass"});

// start serving
PreProxyFS.startPreProxyFS(pacFilePathOrUrl, localPortBind, proxyAuth);

// some code

// stop serving
PreProxyFS.stopPreProxyFS();

```

### Usage as a standalone native program (Windows, macOS, Linux)
It is also possible to download PreProxyFS specifically for your operating system.
* Precondition: Nothing
* Download and extract the PreProxyFS zip file 
  PreProxyFS-Linux-*.zip for Linux;
  PreProxyFS-macOS-*.zip for macOS;  
  PreProxyFS-Windows-*.zip for Windows
* Create a (or more) PreProxyFS.properties file. (see above for example)
* Put the property file somewhere in your filesystem e.g. /home/myuser/PreProxyFS.properties
* Start PreProxyFs on command line with: (Linux, macOS) ./PreProxyFS /home/myuser/PreProxyFS.properties
  and for Windows: PreProxyFS.exe c:/path/to/PreProxyFS.properties
* Output should look like (see above).



