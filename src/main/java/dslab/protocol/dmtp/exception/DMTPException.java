package dslab.protocol.dmtp.exception;

import dslab.protocol.ProtocolException;

public abstract class DMTPException extends ProtocolException {
    public DMTPException(String errorMessage) {
        super(errorMessage);
    }
}
