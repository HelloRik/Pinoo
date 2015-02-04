package com.pinoo.common.utils.security.des;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinoo.common.utils.MD5Utils;

/**
 * DES对称加解密算法
 * 
 * @Filename: AESUtils.java
 * @Version: 1.0
 * @Author: jujun
 * @Email: hello_rik@sina.com
 * 
 */
public class DESUtils {

    private static Logger logger = LoggerFactory.getLogger(DESUtils.class);

    // 加密
    public static String Encrypt(String sSrc, String sKey) throws Exception {
        DES aes = new DES(sKey);
        return aes.encrypt(sSrc.getBytes());
    }

    // 解密
    public static String Decrypt(String sSrc, String sKey) throws Exception {
        DES aes = new DES(sKey);
        return aes.decrypt(sSrc);
    }

    // 加密
    public static String EncryptForString(String sSrc, String sKey) throws Exception {
        DES aes = new DES(sKey);
        return aes.encryptForString(sSrc.getBytes());
    }

    // 解密
    public static String DecryptForString(String sSrc, String sKey) throws Exception {
        DES aes = new DES(sKey);
        return aes.decryptForString(sSrc);
    }

    public static void main(String[] args) throws Exception {

        // System.out.println(Decrypt("znkkRyxbkY3wV5PANXTECR/w5TQTpCsJ0y0O1MOkTYQ=",
        // "d295c9f2d02c7d98a4a766e29e7b66f7"));
        // System.out.println(Decrypt("znkkRyxbkY3wV5PANXTECR/w5TQTpCsJ0y0O1MOkTYQ=",
        // "7XUGk{A`+JBiw=eZyBeuj`(HEgs7^TzA"));
        // System.out.println(Decrypt("McYa9ABV2maq+nupWRuTkqxiASLbd5j1Z5lYN/zbFeI=",
        // "d295c9f2d02c7d98a4a766e29e7b66f7"));
        // System.out.println(Decrypt("McYa9ABV2maq+nupWRuTkqxiASLbd5j1Z5lYN/zbFeI=",
        // "7XUGk{A`+JBiw=eZyBeuj`(HEgs7^TzA"));

        // long id = 21009;
        // long salt = 62375;
        // long random = new Random().nextInt(10000);
        // String value = getEnString(id, salt, "iphone|" + id + "|" + random);
        // System.out.println(value);
        // System.out.println(getDeString(id, salt, value));
        // String value1 = getEnStringForString(id, salt, "iphone|" + id + "|" +
        // random);
        // System.out.println(value1);
        // System.out.println(getDeStringForString(id, salt, value1));
        // System.out.println("&mobile_uid=" + id + "&mobile_secretKey=" +
        // URLEncoder.encode(value, "utf-8"));
        // System.out.println(URLDecoder.decode("yYltBSZ72Zw3tqd7c/5I6B5I9e4VupE0vJkj7lJj62Y=",
        // "utf-8"));

        long id = 21009;
        long salt = 62375;
        long random = new Random().nextInt(10000);
        // String secret = MD5Utils.getMD5Str("iphone|" + id + "|" + random);
        String secret = "iphone|" + id + "|" + random;
        System.out.println(secret);
        String privateKey = MD5Utils.getMD5Str(id + "" + salt);
        System.out.println(privateKey);
        // String value = Encrypt(secret, privateKey);
        String value = "DHS9MBbDHQD3rJgMGg3xrlwxQG+2woVwLq9ekOQYsUI=";
        System.out.println(value);
        String v2 = Decrypt(value, privateKey);
        System.out.println(v2);

    }
}