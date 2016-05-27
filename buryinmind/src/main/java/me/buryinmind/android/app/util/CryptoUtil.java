package me.buryinmind.android.app.util;

import com.tj.xengine.core.utils.XFileUtil;
import com.tj.xengine.core.utils.XStringUtil;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by jason on 2016/3/23.
 */
public abstract class CryptoUtil {

    public static final String KEY_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

//    static {
//        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//    }

    public static String toMd5(String data) {
        return XStringUtil.str2md5(data).toLowerCase();
    }

    public static String toMd5(String data, String salt) {
        return XStringUtil.str2md5(data + salt).toLowerCase();
    }

    public static String toMd5(String data, int bit) {
        String md5 = XStringUtil.str2md5(data).toLowerCase();
        if (bit == 16 && !XStringUtil.isEmpty(md5)) {
            return md5.substring(8, 24);
        } else {
            return md5;
        }
    }

    public static String aesEncrypt(String data, byte[] key, byte[] iv) {
        if (XStringUtil.isEmpty(data))
            return null;
        if (key.length != 16 || iv.length != 16)// 判断key和iv是否为16位
            return null;
        try {
            Cipher cipher= Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
            //使用CBC模式，需要一个向量iv，可增加加密算法的强度
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] result = cipher.doFinal(data.getBytes());
            return parseByte2HexStr(result);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String aesDecrypt(String encrypted, byte[] key, byte[] iv) {
        if (XStringUtil.isEmpty(encrypted))
            return null;
        if (key.length != 16 || iv.length != 16)// 判断key和iv是否为16位
            return null;
        try {
            Cipher cipher= Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
            //使用CBC模式，需要一个向量iv，可增加加密算法的强度
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] result = cipher.doFinal(toByte(encrypted));
            return new String(result, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static File aesEncryptFile(String srcPath, byte[] key, byte[] iv, String encryptPath) {
        if (XStringUtil.isEmpty(srcPath))
            return null;
        if (key.length != 16 || iv.length != 16)// 判断key和iv是否为16位
            return null;
        File encryptFile = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            if (XStringUtil.isEmpty(encryptPath)) {
                encryptFile = File.createTempFile("secret", null);
            } else {
                encryptFile = new File(encryptPath);
                if (encryptFile.isDirectory()) {
                    encryptFile = File.createTempFile("secret", null, encryptFile);
                }
            }
            Cipher cipher= Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            inputStream = new FileInputStream(srcPath);
            outputStream = new FileOutputStream(encryptFile);
            //以加密流写入文件
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);
            byte[] buffer = new byte[XFileUtil.BUFFER_SIZE];
            int r = 0;
            while ((r = cipherInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, r);
            }
            cipherInputStream.close();
            return encryptFile;
        } catch (Exception e) {
            e.printStackTrace();
            if (encryptFile != null)
                encryptFile.delete();
            return null;
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static File aesDecryptFile(String srcPath, byte[] key, byte[] iv, String decryptPath) {
        if (XStringUtil.isEmpty(srcPath))
            return null;
        if (key.length != 16 || iv.length != 16)// 判断key和iv是否为16位
            return null;
        File decryptFile = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            if (XStringUtil.isEmpty(decryptPath)) {
                decryptFile = File.createTempFile("source", null);
            } else {
                decryptFile = new File(decryptPath);
                if (decryptFile.isDirectory()) {
                    decryptFile = File.createTempFile("secret", null, decryptFile);
                }
            }
            Cipher cipher= Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            inputStream = new FileInputStream(srcPath);
            outputStream = new FileOutputStream(decryptFile);
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
            byte [] buffer = new byte [XFileUtil.BUFFER_SIZE];
            int r;
            while ((r = inputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, r);
            }
            cipherOutputStream.close();
            return decryptFile;
        } catch (Exception e) {
            e.printStackTrace();
            if (decryptFile != null)
                decryptFile.delete();
            return null;
        }finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
