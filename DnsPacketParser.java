import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DnsPacketParser {
    public static ParsedResponse parseResponse(byte[] response) throws DnsException {
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

        List<DnsRecord> records = new ArrayList<>();

        // Answers: collect printable RRs
        for (int i = 0; i < ancount; i++) {
            // skip owner name (handles label or pointer)
            int len = buffer.get() & 0xFF;
            if ((len & 0xC0) == 0xC0) {
                buffer.get();
            } else {
                while (len > 0) {
                    buffer.position(buffer.position() + len);
                    len = buffer.get() & 0xFF;
                }
            }

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

            switch (ansType) {
                case 1: // A
                    if (ansRdlength == 4) {
                        String ipAddress = String.format("%d.%d.%d.%d",
                                (rdata[0] & 0xFF), (rdata[1] & 0xFF),
                                (rdata[2] & 0xFF), (rdata[3] & 0xFF));
                        records.add(new DnsRecord(RRType.A, ipAddress, null, ansTtl, isAuthoritative, false));
                    }
                    break;
                case 2: // NS
                {
                    String nsAnswer = decodeDomainName(response, rdataStart);
                    records.add(new DnsRecord(RRType.NS, nsAnswer, null, ansTtl, isAuthoritative, false));
                    break;
                }
                case 5: // CNAME
                {
                    String cnameAnswer = decodeDomainName(response, rdataStart);
                    records.add(new DnsRecord(RRType.CNAME, cnameAnswer, null, ansTtl, isAuthoritative, false));
                    break;
                }
                case 15: // MX
                {
                    ByteBuffer mxBuffer = ByteBuffer.wrap(rdata);
                    int preference = getU16(mxBuffer);
                    String exchange = decodeDomainName(response, rdataStart + 2);
                    records.add(new DnsRecord(RRType.MX, exchange, preference, ansTtl, isAuthoritative, false));
                    break;
                }
                default:
                    // ignore other types
                    break;
            }
        }

        // Authority section: advance buffer, do not collect
        for (int i = 0; i < nscount; i++) {
            // skip owner name
            int len = buffer.get() & 0xFF;
            if ((len & 0xC0) == 0xC0) {
                buffer.get();
            } else {
                while (len > 0) {
                    buffer.position(buffer.position() + len);
                    len = buffer.get() & 0xFF;
                }
            }

            // skip TYPE, CLASS, TTL, RDLENGTH, and RDATA
            getU16(buffer);          // TYPE
            getU16(buffer);          // CLASS
            getU32(buffer);          // TTL
            int rdlen = getU16(buffer);
            buffer.position(buffer.position() + rdlen);
        }

        // Additional section: collect printable RRs and tag as additional
    for (int i = 0; i < arcount; i++) {
        // skip owner name
        int len = buffer.get() & 0xFF;
        if ((len & 0xC0) == 0xC0) {
            buffer.get();
        } else {
            while (len > 0) {
                buffer.position(buffer.position() + len);
                len = buffer.get() & 0xFF;
            }
        }

        int type = getU16(buffer);
        int rrClass = getU16(buffer);
        long ttl = getU32(buffer);
        int rdlen = getU16(buffer);
        int rdataStart = buffer.position();
        byte[] rdata = new byte[rdlen];
        buffer.get(rdata);

        if (rrClass != 1) continue; // only IN

        switch (type) {
            case 1: // A
                if (rdlen == 4) {
                    String ip = String.format("%d.%d.%d.%d",
                            (rdata[0] & 0xFF), (rdata[1] & 0xFF),
                            (rdata[2] & 0xFF), (rdata[3] & 0xFF));
                    records.add(new DnsRecord(RRType.A, ip, null, ttl, isAuthoritative, true));
                }
                break;
            case 2: // NS
            {
                String ns = decodeDomainName(response, rdataStart);
                records.add(new DnsRecord(RRType.NS, ns, null, ttl, isAuthoritative, true));
                break;
            }
            case 5: // CNAME
            {
                String cname = decodeDomainName(response, rdataStart);
                records.add(new DnsRecord(RRType.CNAME, cname, null, ttl, isAuthoritative, true));
                break;
            }
            case 15: // MX
            {
                ByteBuffer mx = ByteBuffer.wrap(rdata);
                int pref = getU16(mx);
                String exch = decodeDomainName(response, rdataStart + 2);
                records.add(new DnsRecord(RRType.MX, exch, pref, ttl, isAuthoritative, true));
                break;
            }
            default:
                // ignore
                break;
        }
    }
    return new ParsedResponse(ancount, arcount, records);
}




    private static int getU16(ByteBuffer buf) {
        return ((buf.get() & 0xFF) << 8) | (buf.get() & 0xFF);
    }

    private static long getU32(ByteBuffer buf) {
        return ((long)(buf.get() & 0xFF) << 24) | ((long)(buf.get() & 0xFF) << 16) | ((long)(buf.get() & 0xFF) << 8) | ((long)(buf.get() & 0xFF));
    }

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
