package dslab.encryption;

import dslab.util.Base64Util;

import javax.crypto.KeyGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ClientChallenge {

    private final byte[] challenge;

    private final byte[] secret;

    private final byte[] iv;

    public ClientChallenge() {
        SecureRandom random = new SecureRandom();
        this.challenge = new byte[32];
        random.nextBytes(challenge);

        this.iv = new byte[16];
        random.nextBytes(iv);

        // Generate an 256-bit AES shared secret key
        try {
            secret = KeyGenerator.getInstance("AES").generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public ClientChallenge(String from) {
        String[] parts = from.split(" ");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid challenge string");
        }

        if (!parts[0].equals("ok")) {
            throw new IllegalArgumentException("Invalid challenge string");
        }

        this.challenge = Base64Util.getInstance().decode(parts[1]);
        this.secret = Base64Util.getInstance().decode(parts[2]);
        this.iv = Base64Util.getInstance().decode(parts[3]);
    }

    /**
     * This method generates a 32-byte random client-challenge, 16-byte secret and 16-byte IV and encodes them in Base64 format.
     *
     * @return a Base64 encoded string containing the encrypted challenge, a secret key and an initialization vector
     */
    public String getChallengeStringAsBase64() {
        return String.format("ok %s %s %s", Base64Util.getInstance().encode(this.challenge), Base64Util.getInstance().encode(this.secret), Base64Util.getInstance().encode(this.iv));
    }

    public byte[] getChallenge() {
        return challenge;
    }

    public byte[] getSecret() {
        return secret;
    }

    public byte[] getIv() {
        return iv;
    }
}
