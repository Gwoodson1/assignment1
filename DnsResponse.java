public class DnsResponse {
    public final byte[] data;
    public final double elapsedTime;
    public final int retriesUsed;

    public DnsResponse(byte[] data, double elapsedTime, int retriesUsed) {
        this.data = data;
        this.elapsedTime = elapsedTime;
        this.retriesUsed = retriesUsed;
    }
}