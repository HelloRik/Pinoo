package com.pinoo.common.utils.security.rsa;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 非对称加密--公私钥
 * 
 * @Filename: RSAUtil.java
 * @Version: 1.0
 * @Author: jujun
 * @Email: hello_rik@sina.com
 * 
 */
public class RSAUtil {

    /**
     * 默认密钥位数
     */
    public final static int DEFAULT_KEY_SIZE = 1024;

    /**
     * 生成公私密钥对
     * 
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return generateKeyPair(DEFAULT_KEY_SIZE);
    }

    /**
     * 生成公私密钥对
     * 
     * @param keySize
     *            密钥位数
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        // 密钥位数
        keyPairGen.initialize(keySize);
        // 生成密钥对
        return keyPairGen.generateKeyPair();
    }

    /**
     * 加密
     * 
     * @param publicKey
     *            公钥
     * @param plainText
     *            明文
     * @return
     * @throws Exception
     */
    public static String doEncrypt(PublicKey publicKey, String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] enBytes = cipher.doFinal(plainText.getBytes());
        return (new BASE64Encoder()).encode(enBytes);
    }

    /**
     * 解密
     * 
     * @param privateKey
     *            私钥
     * @param secretText
     *            密文
     * @return
     * @throws Exception
     */
    public static String doDecrypt(PrivateKey privateKey, String secretText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] secretBytes = (new BASE64Decoder()).decodeBuffer(secretText);
        byte[] deBytes = cipher.doFinal(secretBytes);
        return new String(deBytes);
    }

    /**
     * 得到公钥
     * 
     * @param key
     *            密钥字符串（经过base64编码）
     * @throws Exception
     */
    public static PublicKey getPublicKey(String key) throws Exception {
        byte[] keyBytes = (new BASE64Decoder()).decodeBuffer(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    /**
     * 得到私钥
     * 
     * @param key
     *            密钥字符串（经过base64编码）
     * @throws Exception
     */
    public static PrivateKey getPrivateKey(String key) throws Exception {
        byte[] keyBytes = (new BASE64Decoder()).decodeBuffer(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        return privateKey;
    }

    /**
     * 将公钥或私钥转换成字符串（经过base64编码）
     * 
     * @return
     */
    public static String getKeyString(Key key) throws Exception {
        byte[] keyBytes = key.getEncoded();
        String s = (new BASE64Encoder()).encode(keyBytes);
        return s;
    }

    public static void main(String[] args) throws Exception {

        KeyPair keyPair = generateKeyPair();
        // 公钥
        PublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        // 私钥
        PrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        String publicKeyString = getKeyString(publicKey);
        // System.out.println("public:\n" + publicKeyString);

        String privateKeyString = getKeyString(privateKey);
        // System.out.println("private:\n" + privateKeyString);

        // 明文
        String plainText = "hello_rik@sina.com";

        long startTime = System.currentTimeMillis();
        // 加密
        String secretText = doEncrypt(publicKey, plainText);
        long endTime = System.currentTimeMillis();
        System.out.println("===" + (endTime - startTime));
        System.out.println("密文:\n" + secretText);

        startTime = System.currentTimeMillis();
        plainText = doDecrypt(privateKey, secretText);
        endTime = System.currentTimeMillis();
        System.out.println("===" + (endTime - startTime));
        System.out.println(plainText);

        startTime = System.currentTimeMillis();
        secretText = doEncrypt(publicKey, plainText);
        endTime = System.currentTimeMillis();
        System.out.println("@@@@###" + (endTime - startTime));
    }

}
