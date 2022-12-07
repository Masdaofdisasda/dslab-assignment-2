package dslab.dns;

public class DomainNotFoundException extends RuntimeException {
    public DomainNotFoundException(String message) {
        super(message);
    }
}
