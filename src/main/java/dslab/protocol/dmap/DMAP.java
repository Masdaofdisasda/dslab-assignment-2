package dslab.protocol.dmap;

import dslab.encryption.AES;
import dslab.encryption.ClientChallenge;
import dslab.encryption.RSA;
import dslab.encryption.RSAMode;
import dslab.entity.Message;
import dslab.mailbox.MessageNotFoundException;
import dslab.mailbox.MessageStorageSingleton;
import dslab.mailbox.UserStorageSingleton;
import dslab.protocol.dmap.exception.DMAPErrorException;
import dslab.protocol.dmap.exception.DMAPTerminateConnectionException;
import dslab.util.Base64Util;

import java.util.List;

/**
 * Represents the DMAP2.0 protocol. It is used to communicate with the mailbox server.
 * This class represents the view on the server side.
 */
public class DMAP {

    // Holds the component id of the server (used for obtaining the public key of the server for RSA encryption)
    private final String componentId;

    // Tracks the state of the DMAP protocol connection
    private DMAPStates state = DMAPStates.LOGGED_OUT;

    // Stores the current logged-in user
    private String loggedInUser;

    // Is used for encryption / decryption of the exchanged messages between client and server
    private AES aes;

    // Specifies if the connection is encrypted or not
    private boolean isSecure = false;


    public DMAP(String componentId) {
        this.componentId = componentId;
    }

    public String processInput(String input) throws DMAPErrorException {
        String output;

        // When secured (encrypted) we need to decrypt the input first
        if (isSecure && input != null) {
            input = new String(aes.decrypt(Base64Util.getInstance().decode(input)));
        }

        if (input == null) {
            if (state == DMAPStates.LOGGED_OUT) output = "ok DMAP2.0";
            else
                throw new DMAPErrorException(isSecure ? Base64Util.getInstance().encode(aes.encrypt("error protocol error".getBytes())) : "error protocol error");
        } else if (input.startsWith("startsecure")) {
            output = String.format("ok %s", componentId);
            state = DMAPStates.START_SECURE_RECEIVED;
        } else if (state == DMAPStates.START_SECURE_RECEIVED) {
            // "input" contains the encrypted configuration parameters for the secure connection (challenge, secret, iv)
            // Decrypt the challenge with the servers private key
            RSA rsa = new RSA(componentId, RSAMode.SERVER_DECRYPT_PRIVATE_KEY);
            String decryptedChallenge = new String(rsa.decrypt(Base64Util.getInstance().decode(input)));
            ClientChallenge challenge = new ClientChallenge(decryptedChallenge);

            // Use the challenge to create the AES encryption and decryption objects
            this.aes = new AES(challenge.getSecret(), challenge.getIv());

            // Reply to the client with the received challenge, but now encrypted with the shared secret key (AES)
            output = Base64Util.getInstance().encode(aes.encrypt(String.format("ok %s", Base64Util.getInstance().encode(challenge.getChallenge())).getBytes()));
            state = DMAPStates.CLIENT_CHALLENGE_RECEIVED_AND_ANSWERED;
        } else if (state == DMAPStates.CLIENT_CHALLENGE_RECEIVED_AND_ANSWERED) {
            String response = new String(aes.decrypt(Base64Util.getInstance().decode(input)));
            if (response.startsWith("ok")) {
                isSecure = true;
            }
            output = null;
            state = DMAPStates.LOGGED_OUT;
        } else if (input.startsWith("login")) {
            if (state == DMAPStates.LOGGED_OUT) {
                String[] parts = input.split(" ");
                if (parts.length != 3) {
                    throw new DMAPErrorException(isSecure ? Base64Util.getInstance().encode(aes.encrypt("error invalid input".getBytes())) : "error invalid input");
                }
                String name = parts[1];
                String pw = parts[2];

                if (!UserStorageSingleton.getInstance().doesUserExist(name))
                    throw new DMAPErrorException(isSecure ? Base64Util.getInstance().encode(aes.encrypt("error unknown user".getBytes())) : "error unknown user");

                if (UserStorageSingleton.getInstance().areCredentialsValid(name, pw)) {
                    state = DMAPStates.LOGGED_IN;
                    loggedInUser = name;
                    output = "ok";
                } else {
                    throw new DMAPErrorException(isSecure ? Base64Util.getInstance().encode(aes.encrypt("error wrong password".getBytes())) : "error wrong password");
                }

            } else {
                throw new DMAPErrorException(isSecure ? Base64Util.getInstance().encode(aes.encrypt(String.format("error already logged in with user %s", loggedInUser).getBytes())) : String.format("error already logged in with user %s", loggedInUser));
            }
        } else if (input.equals("list")) {
            if (state == DMAPStates.LOGGED_IN) {
                StringBuilder list = new StringBuilder();
                List<Message> messages = MessageStorageSingleton.getInstance().listMessages(loggedInUser);
                if (messages.size() > 0) {
                    for (Message m : messages) {
                        list.append(m.getId()).append(" ").append(m.getSender()).append(" ").append(m.getSubject()).append("\n");
                    }
                    output = list + "ok";
                } else output = "ok";

            } else {
                throw new DMAPErrorException(isSecure ? Base64Util.getInstance().encode(aes.encrypt("error not logged in".getBytes())) : "error not logged in");
            }
        } else if (input.startsWith("show")) {
            String[] parts = input.split(" ");
            if (parts.length != 2)
                throw new DMAPErrorException(isSecure ? Base64Util.getInstance().encode(aes.encrypt("error invalid input".getBytes())) : "error invalid input");

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
                        .append("data ").append(message.getData()).append("\n");

                output = messageString + "ok";

            } catch (MessageNotFoundException e) {
                throw new DMAPErrorException(isSecure ? Base64Util.getInstance().encode(aes.encrypt("error unknown message id".getBytes())) : "error unknown message id");
            }

        } else if (input.startsWith("delete")) {
            if (state == DMAPStates.LOGGED_IN) {
                String[] parts = input.split(" ");
                if (parts.length != 2)
                    throw new DMAPErrorException(isSecure ? Base64Util.getInstance().encode(aes.encrypt("error invalid input".getBytes())) : "error invalid input");

                try {
                    MessageStorageSingleton.getInstance().deleteMessage(loggedInUser, parts[1]);
                    output = "ok";
                } catch (Exception e) {
                    throw new DMAPErrorException(isSecure ? Base64Util.getInstance().encode(aes.encrypt("error unknown message id".getBytes())) : "error unknown message id");
                }

            } else {
                throw new DMAPErrorException(isSecure ? Base64Util.getInstance().encode(aes.encrypt("error not logged in".getBytes())) : "error not logged in");
            }

        } else if (input.equals("logout")) {
            if (state == DMAPStates.LOGGED_IN) {
                state = DMAPStates.LOGGED_OUT;
                loggedInUser = null;
                output = "ok";
            } else
                throw new DMAPErrorException(isSecure ? Base64Util.getInstance().encode(aes.encrypt("error not logged in".getBytes())) : "error not logged in");

        } else if (input.equals("quit")) {
            // no need for cleanup here, since this object will get garbage collected now anyway
            throw new DMAPTerminateConnectionException(isSecure ? Base64Util.getInstance().encode(aes.encrypt("ok bye".getBytes())) : "ok bye");
        } else {
            throw new DMAPTerminateConnectionException(isSecure ? Base64Util.getInstance().encode(aes.encrypt("error protocol error".getBytes())) : "error protocol error");
        }

        // When secured (encrypted) we need to encrypt the output
        if (isSecure && output != null) {
            output = Base64Util.getInstance().encode(aes.encrypt(output.getBytes()));
        }
        return output;
    }
}

enum DMAPStates {
    START_SECURE_RECEIVED,
    CLIENT_CHALLENGE_RECEIVED_AND_ANSWERED,
    HANDSHAKE_ESTABLISHED,
    LOGGED_IN,
    LOGGED_OUT,
}
