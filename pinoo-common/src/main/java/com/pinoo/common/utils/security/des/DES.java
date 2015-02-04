package com.pinoo.common.utils.security.des;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DES {
    // private final String KEY_GENERATION_ALG = "PBEWITHSHAANDTWOFISH-CBC";

    private final String KEY_GENERATION_ALG = "PBKDF2WithHmacSHA1";

    private final Logger logger = LoggerFactory.getLogger(DES.class);

    private final int HASH_ITERATIONS = 10000;

    private final int KEY_LENGTH = 256;

    private char[] humanPassphrase = null;

    // char[] humanPassphrase = { 'v', 't', 'i', 'o', 'n','s','f','o','t', '.',
    // 'c', 'o', 'm',
    // 'p'};
    private byte[] salt = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF }; // must
                                                                                          // save
                                                                                          // this
                                                                                          // for
                                                                                          // next
                                                                                          // time
                                                                                          // we
                                                                                          // want
                                                                                          // the
                                                                                          // key

    private PBEKeySpec myKeyspec = null;

    private final String CIPHERMODEPADDING = "AES/CBC/PKCS7Padding";

    private SecretKeyFactory keyfactory = null;

    private SecretKey sk = null;

    private SecretKeySpec skforAES = null;

    private byte[] iv = { 0xA, 1, 0xB, 5, 4, 0xF, 7, 9, 0x17, 3, 1, 6, 8, 0xC, 0xD, 91 };

    private IvParameterSpec IV;

    public DES(String key) {

        try {
            humanPassphrase = key.toCharArray();
            myKeyspec = new PBEKeySpec(humanPassphrase, salt, HASH_ITERATIONS, KEY_LENGTH);
            keyfactory = SecretKeyFactory.getInstance(KEY_GENERATION_ALG);
            sk = keyfactory.generateSecret(myKeyspec);
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        } catch (NoSuchAlgorithmException nsae) {
            logger.error("AESdemo", "no key factory support for PBEWITHSHAANDTWOFISH-CBC");
        } catch (InvalidKeySpecException ikse) {
            logger.error("AESdemo", "invalid key spec for PBEWITHSHAANDTWOFISH-CBC");
        }

        // This is our secret key. We could just save this to a file instead of
        // regenerating it
        // each time it is needed. But that file cannot be on the device (too
        // insecure). It could
        // be secure if we kept it on a server accessible through https.
        byte[] skAsByteArray = sk.getEncoded();
        // Log.d("",
        // "skAsByteArray=" + skAsByteArray.length + ","
        // + Base64Encoder.encode(skAsByteArray));
        skforAES = new SecretKeySpec(skAsByteArray, "AES");

        ;

        IV = new IvParameterSpec(iv);

    }

    public String encrypt(byte[] plaintext) throws UnsupportedEncodingException {

        byte[] ciphertext = encrypt(CIPHERMODEPADDING, skforAES, IV, plaintext);

        String base64_ciphertext = DESBase64Encoder.encode(ciphertext);
        return base64_ciphertext;
    }

    public String decrypt(String ciphertext_base64) {
        byte[] s = DESBase64Decoder.decodeToBytes(ciphertext_base64);
        byte[] bytes = decrypt(CIPHERMODEPADDING, skforAES, IV, s);
        String decrypted = new String(bytes);
        return decrypted;
    }

    // Use this method if you want to add the padding manually
    // AES deals with messages in blocks of 16 bytes.
    // This method looks at the length of the message, and adds bytes at the end
    // so that the entire message is a multiple of 16 bytes.
    // the padding is a series of bytes, each set to the total bytes added (a
    // number in range 1..16).
    private byte[] addPadding(byte[] plain) {
        byte plainpad[] = null;
        int shortage = 16 - (plain.length % 16);
        // if already an exact multiple of 16, need to add another block of 16
        // bytes
        if (shortage == 0) {
            shortage = 16;
        }

        // reallocate array bigger to be exact multiple, adding shortage bits.
        plainpad = new byte[plain.length + shortage];
        for (int i = 0; i < plain.length; i++) {
            plainpad[i] = plain[i];
        }
        for (int i = plain.length; i < plain.length + shortage; i++) {
            plainpad[i] = (byte) shortage;
        }
        return plainpad;
    }

    // Use this method if you want to remove the padding manually
    // This method removes the padding bytes
    private byte[] dropPadding(byte[] plainpad) {
        byte plain[] = null;
        int drop = plainpad[plainpad.length - 1]; // last byte gives number of
                                                  // bytes to drop

        // reallocate array smaller, dropping the pad bytes.
        plain = new byte[plainpad.length - drop];
        for (int i = 0; i < plain.length; i++) {
            plain[i] = plainpad[i];
            plainpad[i] = 0; // don't keep a copy of the decrypt
        }
        return plain;
    }

    private byte[] encrypt(String cmp, SecretKey sk, IvParameterSpec IV, byte[] msg) {
        try {
            Cipher c;
            c = Cipher.getInstance(CIPHERMODEPADDING, "BC");
            c.init(Cipher.ENCRYPT_MODE, sk, IV);
            return c.doFinal(msg);
        } catch (NoSuchAlgorithmException e) {
            logger.error("AESdemo", "no cipher getinstance support for " + cmp, e);
        } catch (NoSuchPaddingException e) {
            logger.error("AESdemo", "no cipher getinstance support for padding " + cmp, e);
        } catch (InvalidKeyException e) {
            logger.error("AESdemo", "invalid key exception", e);
        } catch (IllegalBlockSizeException e) {
            logger.error("AESdemo", "illegal block size exception", e);
        } catch (BadPaddingException e) {
            logger.error("AESdemo", "bad padding exception", e);
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private byte[] decrypt(String cmp, SecretKey sk, IvParameterSpec IV, byte[] ciphertext) {
        try {
            Cipher c = Cipher.getInstance(CIPHERMODEPADDING, "BC");
            c.init(Cipher.DECRYPT_MODE, sk, IV);
            return c.doFinal(ciphertext);
        } catch (NoSuchAlgorithmException e) {
            logger.error("AESdemo", "no cipher getinstance support for " + cmp, e);
        } catch (NoSuchPaddingException e) {
            logger.error("AESdemo", "no cipher getinstance support for padding " + cmp, e);
        } catch (InvalidKeyException e) {
            logger.error("AESdemo", "invalid key exception", e);
        } catch (IllegalBlockSizeException e) {
            logger.error("AESdemo", "illegal block size exception", e);
        } catch (BadPaddingException e) {
            logger.error("AESdemo", "bad padding exception", e);
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public String encryptForString(byte[] plaintext) throws UnsupportedEncodingException {

        byte[] ciphertext = encrypt(CIPHERMODEPADDING, skforAES, IV, plaintext);
        return toHex(ciphertext);
    }

    public String decryptForString(String sSrc) throws UnsupportedEncodingException {

        byte[] s = toByte(sSrc);
        String decrypted = new String(decrypt(CIPHERMODEPADDING, skforAES, IV, s));
        return decrypted;
    }

    public static byte[] toByte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
        }
        return result;
    }

    public static String toHex(byte[] buf) {
        if (buf == null) {
            return "";
        }
        StringBuffer result = new StringBuffer(2 * buf.length);
        for (int i = 0; i < buf.length; i++) {
            appendHex(result, buf[i]);
        }
        return result.toString();
    }

    private final static String HEX = "0123456789ABCDEF";

    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
    }

}