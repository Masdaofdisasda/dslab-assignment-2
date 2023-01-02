package dslab.entity;

import dslab.protocol.dmtp.exception.DMTPErrorException;

import java.util.ArrayList;

public class Message {

    private String id;
    private ArrayList<String> recipients;
    private String sender;
    private String subject;
    private String data;
    private String hash;

    public void testMessageReadyForSend() throws DMTPErrorException {
        if (sender == null) throw new DMTPErrorException("error no sender");
        else if (subject == null) throw new DMTPErrorException("error no subject");
        else if (data == null) throw new DMTPErrorException("error no data");
        else if (recipients == null || recipients.isEmpty()) throw new DMTPErrorException("error no recipients");
    }

    public Message() {}

    public Message(String id, ArrayList<String> recipients, String sender, String subject, String data, String hash) {
        this.id = id;
        this.recipients = recipients;
        this.sender = sender;
        this.subject = subject;
        this.data = data;
        this.hash = hash;
    }

    /**
     * Clone constructor for making copies of a message
     *
     * @param that message to copy
     */
    public Message(Message that) {
        this(that.id, that.recipients, that.sender, that.subject, that.data, that.hash);
    }

    public String getId() {
        return id;
    }

    public Message setId(String id) {
        this.id = id;
        return this;
    }

    public ArrayList<String> getRecipients() {
        return recipients;
    }

    public Message setRecipients(ArrayList<String> recipients) {
        this.recipients = recipients;
        return this;
    }

    public String getSender() {
        return sender;
    }

    public Message setSender(String sender) {
        this.sender = sender;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public Message setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getData() {
        return data;
    }

    public Message setData(String data) {
        this.data = data;
        return this;
    }

    public String getHash() {
        return hash;
    }

    public Message setHash(String hash) {
        this.hash = hash;
        return this;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", recipients=" + recipients +
                ", sender='" + sender + '\'' +
                ", subject='" + subject + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
