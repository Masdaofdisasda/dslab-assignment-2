package dslab.protocol.dmtp;

import dslab.protocol.dmtp.exception.DMTPErrorException;

public interface DMTPRecipientValidatorSAM {
    public boolean isValidForThisDomain(String recipient) throws DMTPErrorException;
}
