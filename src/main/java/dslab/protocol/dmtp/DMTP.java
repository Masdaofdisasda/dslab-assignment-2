package dslab.protocol.dmtp;

import dslab.entity.Message;
import dslab.protocol.dmtp.exception.DMTPErrorException;
import dslab.protocol.dmtp.exception.DMTPException;
import dslab.protocol.dmtp.exception.DMTPTerminateConnectionException;

import java.util.ArrayList;


public class DMTP {
    private DMTPStates state = DMTPStates.waiting;
    private Message message;

    private final DMTPRecipientValidatorSAM recipientValidator;

    public DMTP(DMTPRecipientValidatorSAM recipientValidator) {
        this.recipientValidator = recipientValidator;
    }

    public String processInput(String input) throws DMTPException {
        String output = "";

        if (state == DMTPStates.waiting && input == null) {
            output = "ok DMTP";
            state = DMTPStates.begin;

        } else if (input.equals("begin")) {
            if (state == DMTPStates.begin || state == DMTPStates.readyToSend) {
                output = "ok";

                message = new Message();

                state = DMTPStates.receiving;
            } else throw new DMTPErrorException("error protocol error");

        } else if (input.startsWith("to")) {
            if (state == DMTPStates.receiving) {
                if (input.length() <= 3) throw new DMTPErrorException("error no recipients");

                String recipientsString = input.substring(3);

                // check if no just passing whitespaces
                if (recipientsString.trim().length() == 0) throw new DMTPErrorException("error no recipients");

                String[] splitString = recipientsString.split(",");

                ArrayList<String> recipients = new ArrayList<>();

                int recipientCounter = 0;
                // trim spaces from recipients
                for (String recipient : splitString) {
                    if (!validateEmail(recipient.trim()))
                        throw new DMTPErrorException("error invalid email " + recipient.trim());

                    // count recipients from this domain
                    if (recipientValidator.isValidForThisDomain(recipient.trim())) recipientCounter++;
                    recipients.add(recipient.trim());
                }

                message.setRecipients(recipients);

                output = "ok " + recipientCounter;

            } else throw new DMTPErrorException("error protocol error");

        } else if (input.startsWith("from")) {
            if (state == DMTPStates.receiving) {
                if (input.length() <= 5) throw new DMTPErrorException("error no sender");

                String sender = input.substring(5);

                if (sender.trim().length() > 0) {

                    if (!validateEmail(sender.trim())) throw new DMTPErrorException("error invalid sender email");

                    message.setSender(sender);
                    output = "ok";

                } else throw new DMTPErrorException("error no sender");

            } else throw new DMTPErrorException("error protocol error");

        } else if (input.startsWith("subject")) {
            if (state == DMTPStates.receiving) {
                if (input.length() <= 8) throw new DMTPErrorException("error no subject");

                String subject = input.substring(8);
                if (subject.trim().length() > 0) {
                    message.setSubject(subject);
                    output = "ok";

                } else throw new DMTPErrorException("error no subject");

            } else throw new DMTPErrorException("error protocol error");

        } else if (input.startsWith("data")) {
            if (state == DMTPStates.receiving) {
                if (input.length() <= 5) throw new DMTPErrorException("error no data");

                String data = input.substring(5);
                if (data.trim().length() > 0) {
                    message.setData(data);
                    output = "ok";

                } else throw new DMTPErrorException("error no data");

            } else throw new DMTPErrorException("error protocol error");

        }  else if (input.startsWith("hash")) {
            if (state == DMTPStates.receiving) {
                if (input.length() <= 5) throw new DMTPErrorException("error no hash");

                String data = input.substring(5);
                if (data.trim().length() > 0) {
                    message.setHash(data);
                    output = "ok";
                } else throw new DMTPErrorException("error empty hash");

            } else throw new DMTPErrorException("error protocol error");

        } else if (input.equals("send")) {
            if (state == DMTPStates.readyToSend) throw new DMTPErrorException("error protocol error");

            // DMTPErrorException gets thrown if message is not ok
            message.testMessageReadyForSend();
            state = DMTPStates.readyToSend;
            output = "ok";

        } else if (input.equals("quit")) {
            throw new DMTPTerminateConnectionException("ok bye");

        } else {
            // unknown command, terminate connection
            throw new DMTPTerminateConnectionException("error protocol error");
        }

        return output;
    }

    public boolean isMessageReadyToSend() {
        return state == DMTPStates.readyToSend;
    }

    public Message getMessage() {
        return this.message;
    }

    private boolean validateEmail(String email) {
        return email.matches("^(.+)@(.+)$");
    }
}

enum DMTPStates {
    waiting,
    begin,
    receiving,
    readyToSend
}