/**
 * Custom exception class for DNS client errors.
 * 
 * This allows us to clearly separate assignment-specific
 * errors from generic Java exceptions, and ensures we
 * can print error messages in the exact format required.
 */
public class DnsException extends Exception {
    
    // Default constructor
    public DnsException() {
        super();
    }

    // Constructor with a message
    public DnsException(String message) {
        super(message);
    }

    // Constructor with message and cause
    public DnsException(String message, Throwable cause) {
        super(message, cause);
    }

    // Constructor with only cause
    public DnsException(Throwable cause) {
        super(cause);
    }
}
