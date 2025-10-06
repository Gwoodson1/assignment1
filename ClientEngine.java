import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class ClientEngine {
    private ClientConfig config;

    public ClientEngine(ClientConfig config) {
        this.config = config;
    }

    public DnsResponse run() throws DnsException {
        try {
            DatagramSocket socket = new DatagramSocket();

            // Multiply timeout by 1000 to get input timeout in milliseconds
            socket.setSoTimeout(config.timeout * 1000);

            // Converts the server IP so that we can use the getByAddress method
            String[] ipParts = config.serverIP.split("\\.");
            byte[] addressBytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                addressBytes[i] = (byte) Integer.parseInt(ipParts[i]);
            }
            InetAddress serverAddress = InetAddress.getByAddress(addressBytes);

            // Builds the query using the packet builder from above to send to server
            byte[] query = DnsPacketBuilder.buildQuery(config.domainName, DnsPacketBuilder.QType.valueOf(config.queryType));
            DatagramPacket sendPacket = new DatagramPacket(query, query.length, serverAddress, config.port);

            // Allocates a large enough buffer to receive the DNS response
            byte[] buf = new byte[512];
            DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

            // Initialize timer, and retry counter
            long startTime = System.nanoTime();
            int retryCounter = 0;
            boolean success = false;

            // Loop for the desired number of retries, or until we have a successful try
            while (retryCounter <= config.retries && success == false) {
                try {
                    //Try to send the DNS query packet to the server
                    socket.send(sendPacket);
                    socket.receive(receivePacket);

                    // If it reaches this line, then the receipt was successful, and we can exit the loop
                    success = true;

                } catch (SocketTimeoutException e) {
                    retryCounter++;
                    if (retryCounter > config.retries) {
                        socket.close();
                        throw new DnsException("Maximum number of retries " + config.retries + " exceeded");
                    }
                }
            }

            // Calculate the amount of time it took to successfully receive a response
            long endTime = System.nanoTime();
            double elapsedTime = (endTime - startTime) / 1000000000.0;

            byte[] responseData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
            socket.close();
            return new DnsResponse(responseData, elapsedTime, retryCounter);

        } catch (Exception e) {
            throw new DnsException("There was an error with the socket: " + e.getMessage(), e);
        }
    }
}