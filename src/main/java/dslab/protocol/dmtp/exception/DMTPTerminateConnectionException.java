package dslab.protocol.dmtp.exception;

public class DMTPTerminateConnectionException extends DMTPException {
    public DMTPTerminateConnectionException(String errorMessage) {
        super(errorMessage);
    }
}