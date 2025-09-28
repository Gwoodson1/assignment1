# assignment1


## ClientEngine.java
This class implements the UDP send/receive logic of the DNS client. It constructs a DatagramSocket with a configurable timeout, sends the query built by DnsPacketBuilder, and waits for a response using a retry mechanism. Upon success, it records the total elapsed time, prints the results, and returns the exact bytes of the DNS response for further parsing. It adheres strictly to the assignment’s requirements by using InetAddress.getByAddress(), handling timeouts, and using a 512-byte buffer per RFC 1035