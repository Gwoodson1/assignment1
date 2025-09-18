import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

public final class DnsPacketBuilder {
    public enum QType { 
        A(1), NS(2), MX(15);

        public final int code;

        QType(int c)
        {
            code=c;
        }
    }
    public static byte[] buildQuery(String qname, QType qtype) throws DnsException ß{
        int id = ThreadLocalRandom.current().nextInt(0, 0x100000);

        byte[] qnameBytes = encodeQName(qname);
        int headerLen = 12;
        int questionLen = qnameBytes.length + 4; // QTYPE (2 bytes) + QCLASS (2 bytes) 

        ByteBuffer buf = ByteBuffer.allocate(headerLen + questionLen);

        // Header
        putU16(buf, id); // ID
        putU16(buf, 0x0100); // Flags
        putU16(buf, 1); // QDCOUNT
        putU16(buf, 0); // ANCOUNT
        putU16(buf, 0); // NSCOUNT
        putU16(buf, 0); // ARCOUNT

        // Question
        buf.put(qnameBytes); // QNAME
        putU16(buf, qtype.code); // QTYPE
        putU16(buf, 1); // QCLASS (1 = IN)

        return buf.array();

    }

    private static void putU16(ByteBuffer buf, int value) {
        buf.put((byte) ((value >>> 8) & 0xFF));
        buf.put((byte) (value & 0xFF));
    }

    static byte[] encodeQName(String qname) throws DnsException{
        String[] partsOfQuery = qname.split("\\.");

        int totalLength = 1;
        for (String part : partsOfQuery) {
            totalLength += part.length() + 1;
        }

        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        for (String part : partsOfQuery) {
            if (part.length() == 0 || part.length() > 63) {
                throw new DnsException("Each label in the domain name must be 63 characters or less.");
            }
            buffer.put((byte) part.length());
            for (char c : part.toCharArray()) {
                buffer.put((byte) c);
            }
        }

        buffer.put((byte) 0); // Null Byte
        return buffer.array();
    }
}


// byte[] query = DnsPacketBuilder.buildQuery(domain, DnsPacketBuilder.QType.A);