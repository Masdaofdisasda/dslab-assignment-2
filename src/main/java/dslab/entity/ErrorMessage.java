package dslab.entity;

import java.util.ArrayList;

public class ErrorMessage extends Message {
    public ErrorMessage(ArrayList<String> recipients, String sender, String subject, String data) {
        setRecipients(recipients);
        setSender(sender);
        setSubject(subject);
        setData(data);
    }
}
