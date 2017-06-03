package Util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by 1003880 on 2017. 5. 1..
 */
public class Encryptor {
    private static final String HMAC_SHA512 = "HmacSHA512";

    public static String getHmacSha512(String key, String data) {
        Mac sha512_HMAC;
        String result = null;

        try {
            byte[] byteKey = key.getBytes("UTF-8");
            sha512_HMAC = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec keySpec = new SecretKeySpec(byteKey, HMAC_SHA512);
            sha512_HMAC.init(keySpec);
            byte[] macData = sha512_HMAC.doFinal(data.getBytes("UTF-8"));
            result = bytesToHex(macData);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}