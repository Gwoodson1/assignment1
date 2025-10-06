public final class OutputFormatter {
    public static void printQueryBanner(ClientConfig cfg) {
        System.out.println("DnsClient sending request for " + cfg.domainName);
        System.out.println("Server: " + cfg.serverIP);
        System.out.println("Request type: " + cfg.queryType);
    }

    public static void printSuccess(double elapsedSeconds, int retriesUsed) {
        System.out.printf("Response received after %.3f seconds (%d retries)%n",
                elapsedSeconds, retriesUsed);
    }

    public static void printRecords(ParsedResponse pr) {
        if (pr.answerCount > 0) {
            System.out.println("***Answer Section (" + pr.answerCount + " records)***");
            pr.records.stream().filter(r -> !r.inAdditional).forEach(OutputFormatter::printRecordLine);
        } else {
            System.out.println("NOTFOUND");
        }
        if (pr.additionalCount > 0) {
            System.out.println("***Additional Section (" + pr.additionalCount + " records)***");
            pr.records.stream().filter(r -> r.inAdditional).forEach(OutputFormatter::printRecordLine);
        }
    }

    private static void printRecordLine(DnsRecord r) {
        String auth = r.authoritative ? "auth" : "nonauth";
        switch (r.type) {
            case A:
                System.out.printf("IP\t%s\t%d\t%s%n", r.text, r.ttl, auth);
                break;
            case CNAME:
                System.out.printf("CNAME\t%s\t%d\t%s%n", r.text, r.ttl, auth);
                break;
            case NS:
                System.out.printf("NS\t%s\t%d\t%s%n", r.text, r.ttl, auth);
                break;
            case MX:
                System.out.printf("MX\t%s\t%d\t%d\t%s%n", r.text, r.pref, r.ttl, auth);
                break;
        }
    }

    public static void printError(String message) {
        System.out.println("ERROR\t" + message);
    }
}