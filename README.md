# assignment1
Java version 21.0.4
Compile and run instructions:

1) Navigate to assignment1 directory
2) Compile src files using javac *.java     
3) Run DnsClient (eg. java DnsClient -t 10 -r 2 -mx @8.8.8.8 mcgill.ca)


## ClientEngine.java
This class implements the UDP send/receive logic of the DNS client. It constructs a DatagramSocket with a configurable timeout, sends the query built by DnsPacketBuilder, and waits for a response using a retry mechanism. Upon success, it records the total elapsed time, prints the results, and returns the exact bytes of the DNS response for further parsing. It adheres strictly to the assignment’s requirements by using InetAddress.getByAddress(), handling timeouts, and using a 512-byte buffer per RFC 1035