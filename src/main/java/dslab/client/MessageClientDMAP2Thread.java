package dslab.client;

import at.ac.tuwien.dsg.orvell.Shell;
import dslab.encryption.AES;
import dslab.encryption.ClientChallenge;
import dslab.encryption.RSA;
import dslab.encryption.RSAMode;
import dslab.util.Base64Util;
import dslab.util.Config;
import dslab.util.WrappedSocket;

import java.io.IOException;
import java.net.Socket;

public class MessageClientDMAP2Thread extends Thread {

    /* SecureRandom is used for generating the 32-byte random client-challenge and 16-byte random initialization vector (IV) */
    private final Config config;

    private final Shell shell;
    private final ClientChallenge clientChallenge = new ClientChallenge();
    private AES aes;
    private WrappedSocket socket;
    private ClientState clientState = ClientState.BEGIN;
    private boolean isSecure = false;

    public MessageClientDMAP2Thread(Config config, Shell shell) {
        this.config = config;
        this.shell = shell;
    }

    @Override
    public void run() {
        try {
            // Login to the mailbox server securely using DMAP2.0
            socket = new WrappedSocket(new Socket(config.getString("mailbox.host"), config.getInt("mailbox.port")));
            String in;

            while ((in = socket.read()) != null) {
                if (clientState == ClientState.BEGIN && in.startsWith("ok DMAP2.0")) {
                    socket.write("startsecure");
                    clientState = ClientState.START_SECURE;
                } else if (clientState == ClientState.START_SECURE && in.startsWith("ok ") && in.length() > 3) {
                    // Get the componentId from the server response
                    String componentId = in.split(" ")[1];
                    // Use RSA to encrypt the client challenge
                    RSA rsa = new RSA(componentId, RSAMode.CLIENT_ENCRYPT_PUBLIC_KEY);
                    String encryptedChallenge = Base64Util.getInstance().encode(rsa.encrypt(this.clientChallenge.getChallengeStringAsBase64().getBytes()));
                    // Create the AES encryption and decryption objects
                    this.aes = new AES(this.clientChallenge.getSecret(), this.clientChallenge.getIv());
                    // Write to the server the encrypted challenge (RSA)
                    socket.write(encryptedChallenge);
                    clientState = ClientState.HANDSHAKE_CHALLENGE;
                } else if (clientState == ClientState.HANDSHAKE_CHALLENGE) {
                    // Verify the server challenge response with the initial challenge
                    String response = new String(aes.decrypt(Base64Util.getInstance().decode(in)));
                    //shell.out().println("Decrypted challenge response: " + response);

                    // Check if the challenge response is matching the initial challenge
                    // If matching, login the user
                    if (response.contains(Base64Util.getInstance().encode(clientChallenge.getChallenge()))) {
                        isSecure = true;
                        socket.write(Base64Util.getInstance().encode(aes.encrypt("ok".getBytes())));
                        // Create the login string
                        String login = String.format("login %s %s", config.getString("mailbox.user"), config.getString("mailbox.password"));
                        // Encrypt the login string and transmit to server
                        socket.write(Base64Util.getInstance().encode(aes.encrypt(login.getBytes())));
                        clientState = ClientState.LOGIN_EXECUTED;
                    } else {
                        // Close on challenge mismatch
                        socket.close();
                    }
                } else if (clientState == ClientState.LOGIN_EXECUTED) {
                    // Check if the login was successful
                    String response = new String(aes.decrypt(Base64Util.getInstance().decode(in)));
                    if (response.contains("ok")) {
                        clientState = ClientState.LOGGED_IN;
                    } else {
                        // Close on login failure
                        socket.close();
                    }
                } else if (clientState == ClientState.LOGGED_IN) {
                    // Decrypt the server response
                    String response = new String(aes.decrypt(Base64Util.getInstance().decode(in)));
                    // Print the server response
                    shell.out().println();
                    shell.out().println(response);
                } else {
                    socket.write(Base64Util.getInstance().encode(aes.encrypt("error protocol error".getBytes())));
                    socket.close();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void inbox() {
        socket.write(Base64Util.getInstance().encode(aes.encrypt("list".getBytes())));
    }


    /**
     * ClientState is used to keep track of the handshake and login progress.
     */
    private enum ClientState {
        // Initial state
        BEGIN,

        // Start secure command has been sent to the server
        START_SECURE,

        // Challenge has been sent to the server
        HANDSHAKE_CHALLENGE,

        // Handshake has been completed between the client and the server
        HANDSHAKE_ESTABLISHED,

        // When the login command has been sent to the server
        LOGIN_EXECUTED,

        // Login has been completed and the client is ready to send commands
        LOGGED_IN
    }

}
