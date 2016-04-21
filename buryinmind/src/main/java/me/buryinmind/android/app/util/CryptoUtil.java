package me.buryinmind.android.app.util;

import com.tj.xengine.core.utils.XStringUtil;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

/**
 * Created by jason on 2016/3/23.
 */
public abstract class CryptoUtil {

    public static final String KEY_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final byte[] key = "0123456789abcdef".getBytes();
    private static final SecretKey secretKey = new SecretKeySpec(key, KEY_ALGORITHM);

//    static {
//        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//    }

    public static String toMd5(String data) {
        return XStringUtil.str2md5(data).toLowerCase();
    }

    public static String toMd5(String data, String salt) {
        return XStringUtil.str2md5(data + salt).toLowerCase();
    }

    public static String encrypt(String data) {
        if (XStringUtil.isEmpty(data))
            return null;
        try {
            Cipher cipher= Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] result = cipher.doFinal(data.getBytes());
            return parseByte2HexStr(result);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decrypt(String encrypted) {
        if (XStringUtil.isEmpty(encrypted))
            return null;
        try {
            Cipher cipher= Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] result = cipher.doFinal(toByte(encrypted));
            return new String(result, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 字符串转字节数组
     */
    private static byte[] toByte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
        }
        return result;
    }

    /**
     * 字节转16进制数组
     */
    private static String parseByte2HexStr(byte buf[]) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
