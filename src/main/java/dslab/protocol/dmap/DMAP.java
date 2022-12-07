package dslab.protocol.dmap;

import dslab.entity.Message;
import dslab.mailbox.MessageNotFoundException;
import dslab.mailbox.MessageStorageSingleton;
import dslab.mailbox.UserStorageSingleton;
import dslab.protocol.dmap.exception.DMAPErrorException;
import dslab.protocol.dmap.exception.DMAPTerminateConnectionException;

import java.util.List;

public class DMAP {
    private DMAPStates state = DMAPStates.loggedOut;
    private String loggedInUser;

    public String processInput(String input) throws DMAPErrorException {
        String output;

        if (input == null) {
            if (state == DMAPStates.loggedOut) output = "ok DMAP";
            else throw new DMAPErrorException("error protocol error");
        } else if (input.startsWith("login")) {
            if (state == DMAPStates.loggedOut) {
                String[] parts = input.split(" ");
                if (parts.length != 3) throw new DMAPErrorException("error invalid input");
                String name = parts[1];
                String pw = parts[2];

                if (!UserStorageSingleton.getInstance().doesUserExist(name))
                    throw new DMAPErrorException("error unknown user");

                if (UserStorageSingleton.getInstance().areCredentialsValid(name, pw)) {
                    state = DMAPStates.loggedIn;
                    loggedInUser = name;
                    output = "ok";
                } else throw new DMAPErrorException("error wrong password");

            } else throw new DMAPErrorException("error already logged in with user " + loggedInUser);
        } else if (input.equals("list")) {
            if (state == DMAPStates.loggedIn) {
                StringBuilder list = new StringBuilder();
                List<Message> messages = MessageStorageSingleton.getInstance().listMessages(loggedInUser);
                if (messages.size() > 0) {
                    for (Message m : messages) {
                        list.append(m.getId()).append(" ").append(m.getSender()).append(" ").append(m.getSubject()).append("\n");
                    }
                    output = list.substring(0, list.toString().length() - 1); // cut last linebreak from output string
                } else output = "You have no stored messages!";

            } else throw new DMAPErrorException("error not logged in");
        } else if (input.startsWith("show")) {
            String[] parts = input.split(" ");
            if (parts.length != 2) throw new DMAPErrorException("error invalid input");

            try {
                Message message = MessageStorageSingleton.getInstance().getMessage(loggedInUser, parts[1]);
                List<String> recipients = message.getRecipients();
                StringBuilder messageString = new StringBuilder();
                messageString.append("from ").append(message.getSender()).append("\n");
                messageString.append("to ");
                for (int i = 0; i < recipients.size(); i++) {
                    messageString.append(recipients.get(i));
                    if (i < (recipients.size() - 1)) messageString.append(", ");
                }
                messageString.append("\n");

                messageString
                        .append("subject ").append(message.getSubject()).append("\n")
                        .append("data ").append(message.getData());

                output = messageString.toString();

            } catch (MessageNotFoundException e) {
                throw new DMAPErrorException("error unknown message id");
            }

        } else if (input.startsWith("delete")) {
            if (state == DMAPStates.loggedIn) {
                String[] parts = input.split(" ");
                if (parts.length != 2) throw new DMAPErrorException("error invalid input");

                try {
                    MessageStorageSingleton.getInstance().deleteMessage(loggedInUser, parts[1]);
                    output = "ok";
                } catch (Exception e) {
                    throw new DMAPErrorException("error unknown message id");
                }

            } else throw new DMAPErrorException("error not logged in");
        } else if (input.equals("logout")) {
            if (state == DMAPStates.loggedIn) {
                state = DMAPStates.loggedOut;
                loggedInUser = null;
                output = "ok";

            } else throw new DMAPErrorException("error not logged in");

        } else if (input.equals("quit")) {
            // no need for cleanup here, since this object will get garbage collected now anyway
            throw new DMAPTerminateConnectionException("ok bye");
        } else {
            throw new DMAPTerminateConnectionException("error protocol error");
        }

        return output;
    }
}

enum DMAPStates {
    loggedIn,
    loggedOut,
}