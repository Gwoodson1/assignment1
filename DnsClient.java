
public class DnsClient {
    public static void main(String[] args) {
        try {
            ClientConfig config = parseArguments(args);
            // ClientEngine engine = new ClientEngine(config);
            // engine.run();
            // Debug: print all parsed values
            System.out.println("=== Parsed ClientConfig ===");
            System.out.println("Timeout:    " + config.timeout);
            System.out.println("Retries:    " + config.retries);
            System.out.println("Port:       " + config.port);
            System.out.println("Query Type: " + config.queryType);
            System.out.println("Server IP:  " + config.serverIP);
            System.out.println("Domain:     " + config.domainName);
            System.out.println("===========================");
        } catch (DnsException e) {
            System.out.println("ERROR\t" + e.getMessage());
        }
    }

    // Add  'throws DnsException' after it's defined
    private static ClientConfig parseArguments(String[] args) throws DnsException {
        // defaults
        int timeout = 5;
        int retries = 3;
        int port = 53;
        String queryType = "A";   // default
        String server = null;
        String name = null;

        // iterate args
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-t":
                    timeout = Integer.parseInt(args[++i]);
                    break;
                case "-r":
                    retries = Integer.parseInt(args[++i]);
                    break;
                case "-p":
                    port = Integer.parseInt(args[++i]);
                    break;
                case "-mx":
                    queryType = "MX";
                    break;
                case "-ns":
                    queryType = "NS";
                    break;
                default:
                    if (arg.startsWith("@")) {
                        server = arg.substring(1);
                    } else {
                        name = arg;
                    }
                    break;
            }
        }

        //validate
        if (server == null || name == null) {
            throw new DnsException("Incorrect input syntax: missing server or domain name");
        }

        return new ClientConfig(timeout, retries, port, queryType, server, name);
    }

}