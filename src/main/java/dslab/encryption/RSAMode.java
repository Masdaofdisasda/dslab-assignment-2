package dslab.encryption;

public enum RSAMode {
    // Used by the client to encrypt the client-challenge
    CLIENT_ENCRYPT_PUBLIC_KEY,

    // Used by the server to decrypt the client-challenge
    SERVER_DECRYPT_PRIVATE_KEY
}
