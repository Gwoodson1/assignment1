public class ClientConfig {
    public final int timeout;
    public final int retries;
    public final int port;
    public final String queryType;
    public final String serverIP;
    public final String domainName;

    public ClientConfig(int timeout, int retries, int port, 
                        String queryType, String serverIP, String domainName) {
        this.timeout = timeout;
        this.retries = retries;
        this.port = port;
        this.queryType = queryType;
        this.serverIP = serverIP;
        this.domainName = domainName;
    }
}