package dslab.util;

import dslab.entity.Message;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

public class MessageHasher {

  public byte[] calculateHash(Message message) {
    SecretKeySpec key = loadSharedSecret();
    Mac mac = createMac(key);
    byte[] bytes = prepareInput(message);

    return calculateHash(mac, bytes);
  }

  public String encodeHash(byte[] hash) {
    return Base64.getEncoder().encodeToString(hash);
  }

  public boolean verify(Message message) {
    byte[] binaryHash = calculateHash(message);
    String encodedHash = encodeHash(binaryHash);
    return Objects.equals(encodedHash, message.getHash());
  }

  private static byte[] calculateHash(Mac mac, byte[] bytes) {
    mac.update(bytes);
    return mac.doFinal();
  }

  private static byte[] prepareInput(Message message) {
    String msg = String.join("\n", message.getSender(), message.getRecipients().toString(), message.getSubject(), message.getData()); //todo
    byte[] bytes = msg.getBytes();
    return bytes;
  }

  private static Mac createMac(SecretKeySpec key) {
    Mac mac;
    try {
      mac = Mac.getInstance(key.getAlgorithm());
      mac.init(key);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (InvalidKeyException e) {
      throw new RuntimeException(e);
    }
    return mac;
  }

  private static SecretKeySpec loadSharedSecret() {
    SecretKeySpec key;
    try {
      key = Keys.readSecretKey(new File("keys/hmac.key"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return key;
  }
}
