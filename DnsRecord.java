public final class DnsRecord {
  public final RRType type;
  public final String text;
  public final Integer pref;     
  public final long ttl;
  public final boolean authoritative;
  public final boolean inAdditional;
  public DnsRecord(RRType type, String text, Integer pref, long ttl, boolean authoritative, boolean inAdditional) {
    this.type = type; this.text = text; this.pref = pref; this.ttl = ttl;
    this.authoritative = authoritative; this.inAdditional = inAdditional;
  }
}