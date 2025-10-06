import java.util.List;
public final class ParsedResponse {
  public final int answerCount;
  public final int additionalCount;
  public final List<DnsRecord> records;
  public ParsedResponse(int answerCount, int additionalCount, List<DnsRecord> records) {
    this.answerCount = answerCount; this.additionalCount = additionalCount; this.records = records;
  }
}