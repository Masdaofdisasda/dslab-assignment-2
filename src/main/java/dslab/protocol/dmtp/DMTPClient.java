package dslab.protocol.dmtp;

import dslab.entity.Domain;
import dslab.entity.Message;
import dslab.protocol.dmtp.exception.DMTPClientException;
import dslab.util.WrappedSocket;

import java.io.IOException;
import java.net.Socket;

public class DMTPClient {
    private final Message message;
    private final Domain recipientDomain;

    public DMTPClient(Message message, Domain recipientDomain) {
        this.message = message;
        this.recipientDomain = recipientDomain;
    }

    // send a DMTP message to next Transfer or Mailbox Server
    public void sendMessage() throws DMTPClientException, IOException {

        WrappedSocket socket = new WrappedSocket(new Socket(recipientDomain.getHost(), recipientDomain.getPort()));

        String in;
        DMTPClientStates state = DMTPClientStates.begin;

        while ((in = socket.read()) != null) {

            if (in.equals("ok DMTP")) {
                if (state == DMTPClientStates.begin) {
                    socket.write("begin");
                    state = DMTPClientStates.from;
                }
            } else if (in.startsWith("ok")) {
                String out = "";
                if (state == DMTPClientStates.from) {
                    out = "from " + message.getSender();
                    state = DMTPClientStates.to;
                } else if (state == DMTPClientStates.to) {
                    // you should probably check if the server responds with the correct amount
                    // of recipients after this
                    out = "to " + String.join(",", message.getRecipients());
                    state = DMTPClientStates.subject;
                } else if (state == DMTPClientStates.subject) {
                    out = "subject " + message.getSubject();
                    state = DMTPClientStates.data;
                } else if (state == DMTPClientStates.data) {
                    out = "data " + message.getData();
                    state = DMTPClientStates.hash;
                } else if (state == DMTPClientStates.hash) {
                    out = "hash " + message.getHash();
                    state = DMTPClientStates.send;
                } else if (state == DMTPClientStates.send) {
                    out = "send";
                    state = DMTPClientStates.sent;
                } else if (state == DMTPClientStates.sent) {
                    out = "quit";
                    socket.write(out);
                    socket.close();

                    break;
                }

                socket.write(out);
            } else throw new DMTPClientException("Error forwarding message to " + recipientDomain.getName());
        }
    }
}

enum DMTPClientStates {
    waiting,
    begin,
    from,
    to,
    subject,
    data,
    hash,
    send,
    sent,
    quit
}