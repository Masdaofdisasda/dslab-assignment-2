package dslab.encryption;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * This class is used for encrypting and decrypting data using the "RSA" algorithm.
 */
public class RSA {

    // Defines if a public or private key is used for encryption/decryption operation
    private final RSAMode mode;

    // Public or Private key depending on the given RSAMode
    private final Key key;
    // Contains the Cipher for encrypting and decrypting data
    private final Cipher cipher;

    /**
     * Creates a new RSA instance for encrypting and decrypting data using the "RSA" algorithm.
     * The given key is used for encryption/decryption depending on the given mode.
     * The key is read from the file system.
     *
     * @param componentId The componentId of the remote server
     * @param mode Defines if a public or private key is used for encryption/decryption operation
     */
    public RSA(String componentId, RSAMode mode) {
        this.mode = mode;

        FileInputStream fis;
        byte[] key;

        // Read in the public/private key from the file
        try {
            if (mode == RSAMode.CLIENT_ENCRYPT_PUBLIC_KEY) {
                fis = new FileInputStream(String.format("keys/client/%s_pub.der", componentId));
            } else {
                fis = new FileInputStream(String.format("keys/server/%s.der", componentId));
            }
            key = fis.readAllBytes();
            fis.close();
        } catch (FileNotFoundException e) {
            // If the file name is not found
            throw new RuntimeException("Could not find the key file", e);
        } catch (IOException e) {
            // If the file is not readable, or if an I/O error occurs ...
            throw new RuntimeException("Could not read the key file", e);
        }

        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            if (mode == RSAMode.CLIENT_ENCRYPT_PUBLIC_KEY) {
                // Initialize the key instance
                X509EncodedKeySpec ks = new X509EncodedKeySpec(key);
                this.key = kf.generatePublic(ks);
                this.cipher.init(Cipher.ENCRYPT_MODE, this.key);
            } else {
                // Initialize the key instance
                PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(key);
                this.key = kf.generatePrivate(ks);
                this.cipher.init(Cipher.DECRYPT_MODE, this.key);
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException e) {
            System.out.println(e.getMessage());
             throw new RuntimeException(e);
        }
    }

    public byte[] encrypt(byte[] data) {
        try {
            return this.cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] decrypt(byte[] data) {
        if (mode == RSAMode.CLIENT_ENCRYPT_PUBLIC_KEY) {
            throw new RuntimeException("Cannot decrypt data with a public key");
        }

        try {
            System.out.println("Decrypting data with private key");
            return this.cipher.doFinal(data);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

