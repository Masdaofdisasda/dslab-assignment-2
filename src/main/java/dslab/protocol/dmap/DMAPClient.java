package dslab.protocol.dmap;

import dslab.encryption.AES;
import dslab.encryption.ClientChallenge;
import dslab.encryption.RSA;
import dslab.encryption.RSAMode;
import dslab.protocol.dmap.exception.DMAPErrorException;
import dslab.util.Base64Util;
import dslab.util.WrappedSocket;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DMAPClient {

    private final WrappedSocket socket;

    // Client challenge + AES configuration
    private final ClientChallenge clientChallenge = new ClientChallenge();

    // Used for the AES encryption / decryption
    private AES aes;

    // Used for the RSA encryption
    private RSA rsa;

    public DMAPClient(String domain, int port) throws IOException {
        socket = new WrappedSocket(new Socket(domain, port));
        if (!Objects.equals(socket.read(), "ok DMAP2.0")) {
            throw new DMAPErrorException("Could not connect to DMAP Server with socket: " + socket);
        }
    }

    public void login(String username, String password) throws IOException {
        String input;
        // Tell the server that we want to communicate over an encrypted connection
        socket.write("startsecure");
        // Read the response and extract the component id
        if (!(input = socket.read()).startsWith("ok ") || !(input.length() > 3)) {
            throw new DMAPErrorException("Could not start secure connection");
        }
        // Extract the component id
        String componentId = input.split(" ")[1];
        RSA rsa = new RSA(componentId, RSAMode.CLIENT_ENCRYPT_PUBLIC_KEY);
        String encryptedChallenge = Base64Util.getInstance().encode(rsa.encrypt(this.clientChallenge.getChallengeStringAsBase64().getBytes()));
        // Create the AES encryption and decryption objects
        this.aes = new AES(this.clientChallenge.getSecret(), this.clientChallenge.getIv());
        // Write to the server the encrypted challenge (RSA)
        socket.write(encryptedChallenge);

        // Verify the server challenge response with the initial challenge
        input = socket.read();
        String response = new String(aes.decrypt(Base64Util.getInstance().decode(input)));
        // Check if the challenge response is matching the initial challenge
        if (!response.contains(Base64Util.getInstance().encode(clientChallenge.getChallenge()))) {
            throw new DMAPErrorException("Handshake failed due to challenge mismatch");
        }
        // Send confirmation to the server that the challenge was correct and the handshake is complete
        socket.write(Base64Util.getInstance().encode(aes.encrypt("ok".getBytes())));

        // LOGIN PART
        String login = String.format("login %s %s", username, password);
        // Encrypt the login string and transmit to server
        socket.write(Base64Util.getInstance().encode(aes.encrypt(login.getBytes())));
        response = new String(aes.decrypt(Base64Util.getInstance().decode(socket.read())));
        if (!response.equals("ok")) {
            throw new DMAPErrorException("Could not login");
        }

        // User is logged in and can now send commands securely
    }

    public List<String> list() throws IOException {
        // Send the list (encrypted) command
        socket.write(Base64Util.getInstance().encode(aes.encrypt("list".getBytes())));
        String response = socket.read();
        // Decrypt the response
        response = new String(aes.decrypt(Base64Util.getInstance().decode(response)));

        // Split the response into multiple messages
        List<String> messages = new ArrayList<>(List.of(response.split("\n")));

        if (messages.size() == 1 && messages.get(0).equals("ok")) {
            return List.of("You have no messages.");
        }

        // If there is at least one message, remove the "ok" termination message
        messages.remove("ok");

        return messages;
    }

    public String show(String messageId) throws IOException {
        String command = String.format("show %s", messageId);
        socket.write(Base64Util.getInstance().encode(aes.encrypt(command.getBytes())));
        String response = socket.read();
        // Decrypt the response
        response = new String(aes.decrypt(Base64Util.getInstance().decode(response)));

        // Return the message without the "ok" termination message
        return response.split("\nok")[0];
    }

    public void delete(String messageId) throws IOException {
        String command = String.format("delete %s", messageId);
        // Send the delete command (encrypted)
        socket.write(Base64Util.getInstance().encode(aes.encrypt(command.getBytes())));

        // Read the response from the server
        String response = socket.read();
        // Decrypt the response
        response = new String(aes.decrypt(Base64Util.getInstance().decode(response)));

        // Check if the response is "ok" and throw an exception if not
        if (!response.equals("ok")) {
            throw new DMAPErrorException("An error occurred when deleting message " + messageId);
        }
    }

    public void logout() throws IOException {
        socket.write("logout");
        if (!Objects.equals(socket.read(), "ok")) {
            throw new DMAPErrorException("Could not logout");
        }
    }

    public void quit() {
        socket.close();
    }
}
