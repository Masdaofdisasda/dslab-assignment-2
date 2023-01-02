package dslab.util;

import java.util.Base64;

public class Base64Util {

    private final static Base64Util INSTANCE = new Base64Util();

    private Base64Util() {
    }

    public static Base64Util getInstance() {
        return INSTANCE;
    }

    public String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }
}
