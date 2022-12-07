package dslab.protocol.dmap.exception;

import dslab.protocol.ProtocolException;

public abstract class DMAPException extends ProtocolException {
    public DMAPException(String message) {
        super(message);
    }
}
