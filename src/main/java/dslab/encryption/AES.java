package dslab.encryption;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;

/**
 * This class is used for encrypting and decrypting data using the "AES" algorithm.
 */
public class AES {

    // Holds the shared secret for the client server communication (must be kept secret at any time)
    private final SecretKey secretKey;

    // Holds the initialization vector (IV) for the client server communication and is used to increase the cryptographic variance for increased security
    private final IvParameterSpec iv;

    // Holds the cipher for encrypting / decrypting data
    private final Cipher cipher;

    /**
     * Creates a new AES instance for encrypting or decrypting data using the "AES" algorithm.
     * The given secretKey and iv are used for init the cipher and further for encryption/decryption.
     * The secretKey and iv are transmitted initially between the client and the server via the "RSA" algorithm.
     *
     * @param secretKey The shared secret for the client server communication (must be kept secret at any time)
     * @param iv        The initialization vector (IV) for the client server communication and is used to increase the cryptographic variance for increased security
     */
    public AES(byte[] secretKey, byte[] iv) {

        // Create a SecretKey instance from the secret key bytes for the "AES" algorithm (symmetric encryption)
        this.secretKey = new SecretKeySpec(secretKey, "AES");
        // Create an initialization vector instance for the "AES" algorithm
        this.iv = new IvParameterSpec(iv);

        // Initialize the cipher for encrypting / decrypting data
        try {
            this.cipher = Cipher.getInstance("AES/CTR/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encrypts the given data using the "AES" algorithm.
     *
     * @param data The data to encrypt
     * @return The encrypted data
     */
    public byte[] encrypt(byte[] data) {
        try {
            this.cipher.init(Cipher.ENCRYPT_MODE, this.secretKey, this.iv);
            // Encrypt the data using the cipher
            return this.cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decrypts the given data using the "AES" algorithm.
     *
     * @param data The data to decrypt
     * @return The decrypted data
     */
    public byte[] decrypt(byte[] data) {
        try {
            this.cipher.init(Cipher.DECRYPT_MODE, this.secretKey, this.iv);
            // Decrypt the data using the cipher
            return this.cipher.doFinal(data);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
