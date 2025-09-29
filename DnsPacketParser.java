import java.nio.ByteBuffer;

public class DnsPacketParser {
    public static void parseResponse(byte[] response) throws DnsException {
        ByteBuffer buffer = ByteBuffer.wrap(response);

        // Header information (to be used later)
        int id = getU16(buffer);
        int flags = getU16(buffer);
        int qdcount = getU16(buffer);
        int ancount = getU16(buffer);
        int nscount = getU16(buffer);
        int arcount = getU16(buffer);

        // To determine whether to output auth or nonauth
        boolean isAuthoritative = (flags & 0x0400) != 0;

        // We want to see hte contents of the header (FOR DEBUG PURPOSES)
        // System.out.println("=== DNS HEADER ===");
        // System.out.println("ID: " + id);
        // System.out.println("Flags: 0x" + Integer.toHexString(flags));
        // System.out.println("Questions: " + qdcount);
        // System.out.println("Answers: " + ancount);
        // System.out.println("Authority RRs: " + nscount);
        // System.out.println("Additional RRs: " + arcount);
        // System.out.println("Authoritative: " + isAuthoritative);

        // Decode the flags and ensure no errors
        boolean qr = (flags & 0x8000) != 0;
        int rcode = (flags & 0x000F);
        System.out.println("QR (response?): " + qr);
        System.out.println("RCODE (return code): " + rcode);
        if (rcode != 0) {
            throw new DnsException("Server returned error code " + rcode);
        }

        // Skip over the question(s) because we don't need to display that to the user
        for (int i = 0; i < qdcount; i++) {
            // Skip QNAME
            while (true) {
                int len = buffer.get() & 0xFF;
                if (len == 0) break; // end of name
                buffer.position(buffer.position() + len);
            }
            // Skip QTYPE + QCLASS total of 4 btyes
            buffer.position(buffer.position() + 4);
        }

        // Now loop through and print the answers
        System.out.println("\n=== DNS ANSWERS ===");
        for (int i = 0; i < ancount; i++) {
            int len = buffer.get() & 0xFF;
            if ((len & 0xC0) == 0xC0) {
                // This means that we hit a pointer compression case, so skip the second pointer byte for now
                buffer.get();
            } else {
                while (len > 0) {
                    buffer.position(buffer.position() + len);
                    len = buffer.get() & 0xFF;
                }
            }

            // Get the fixed fields from the answer
            int ansType = getU16(buffer);
            int ansClass = getU16(buffer);
            long ansTtl = getU32(buffer);
            int ansRdlength = getU16(buffer);
            int rdataStart = buffer.position();
            byte[] rdata = new byte[ansRdlength];
            buffer.get(rdata);

            if (ansClass != 1) {
                throw new DnsException("Unexpected CLASS value: " + ansClass);
            }

            // Now check each type of record and parse accordingly
            // DEBUG ANSWER TYPE
            System.out.println("DEBUG: Record Type = " + ansType + ", Length = " + ansRdlength);
            switch (ansType) {
                // Case 1 = A record
                case 1:
                    if (ansRdlength == 4) {
                        String ipAddress = String.format("%d.%d.%d.%d", (rdata[0] & 0xFF), (rdata[1] & 0xFF), (rdata[2] & 0xFF), (rdata[3] & 0xFF));
                        System.out.printf("IP\t%s\t%d\t%s%n", ipAddress, ansTtl, isAuthoritative ? "auth" : "nonauth");
                    }   
                    break;
                // Case 2 = NS record
                case 2:
                    String nsAnswer = decodeDomainName(response, rdataStart);
                    System.out.printf("NS\t%s\t%d\t%s%n", nsAnswer, ansTtl, isAuthoritative ? "auth" : "nonauth");
                    break;
                // Case 5 = CNAME record
                case 5:
                    String cnameAnswer = decodeDomainName(response, rdataStart);
                    System.out.printf("CNAME\t%s\t%d\t%s%n", cnameAnswer, ansTtl, isAuthoritative ? "auth" : "nonauth");
                    break;
                // Case 15 = MX record
                case 15:
                    ByteBuffer mxBuffer = ByteBuffer.wrap(rdata);
                    int preference = getU16(mxBuffer);
                    String exchange = decodeDomainName(response, rdataStart + 2);
                    System.out.printf("MX\t%s\t%d\t%d\t%s%n", exchange, preference, ansTtl, isAuthoritative ? "auth" : "nonauth");
                    break;
                // All other types should be ignored
                default:
                    break;
            }
        }
    }




    private static int getU16(ByteBuffer buf) {
        return ((buf.get() & 0xFF) << 8) | (buf.get() & 0xFF);
    }

    private static long getU32(ByteBuffer buf) {
        return ((long)(buf.get() & 0xFF) << 24) | ((long)(buf.get() & 0xFF) << 16) | ((long)(buf.get() & 0xFF) << 8) | ((long)(buf.get() & 0xFF));
    }

    // Currently does not handle compression **MIGHT NEED TO ADD LATER**
    private static String decodeDomainName(byte[] data, int offset) throws DnsException {
        StringBuilder name = new StringBuilder();
        int jumps = 0;  // guard against infinite loops

        while (true) {
            int len = data[offset] & 0xFF;

            if (len == 0) {          // end of name
                offset++;
                break;
            }

            if ((len & 0xC0) == 0xC0) {   // pointer (starts with bits 11)
                int pointer = ((len & 0x3F) << 8) | (data[offset + 1] & 0xFF);
                if (jumps++ > 5) throw new DnsException("Too many compression jumps");
                // recursively decode from pointer target
                name.append(decodeDomainName(data, pointer));
                return name.toString();
            }

            offset++;
            if (offset + len > data.length)
                throw new DnsException("Invalid domain name offset");

            for (int i = 0; i < len; i++) {
                name.append((char) data[offset + i]);
            }
            offset += len;

            // add dot unless last label
            if (data[offset] != 0) name.append('.');
        }

        return name.toString();
    }
}
