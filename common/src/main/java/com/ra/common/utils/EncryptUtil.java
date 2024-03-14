package com.ra.common.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
/**
 * 加密工具类，包含MD5,BASE64,SHA,CRC32
 * 
 */
public final class EncryptUtil {

    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String KEY_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";//默认的加密算法

    /**
     * MD5加密
     * 
     * @param bytes
     *            an array of byte.
     * @return a {@link java.lang.String} object.
     */
    public static String encodeMD5(final byte[] bytes) {
        return DigestUtils.md5Hex(bytes).toUpperCase();
    }

    /**
     * MD5加密，默认UTF-8
     * 
     * @param str
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String encodeMD5(final String str) {
        return encodeMD5(str, DEFAULT_CHARSET);
    }

    /**
     * MD5加密
     * 
     * @param str
     *            a {@link java.lang.String} object.
     * @param charset
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String encodeMD5(final String str, final String charset) {
        if (str == null) {
            return null;
        }
        try {
            byte[] bytes = str.getBytes(charset);
            return encodeMD5(bytes);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * SHA加密
     * 
     * @param bytes
     *            an array of byte.
     * @return a {@link java.lang.String} object.
     */
    public static String encodeSHA(final byte[] bytes) {
        return DigestUtils.sha512Hex(bytes);
    }

    /**
     * SHA加密
     * 
     * @param str
     *            a {@link java.lang.String} object.
     * @param charset
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String encodeSHA(final String str, final String charset) {
        if (str == null) {
            return null;
        }
        try {
            byte[] bytes = str.getBytes(charset);
            return encodeSHA(bytes);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * SHA加密,默认utf-8
     * 
     * @param str
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String encodeSHA(final String str) {
        return encodeSHA(str, DEFAULT_CHARSET);
    }

    /**
     * BASE64加密
     * 
     * @param bytes
     *            an array of byte.
     * @return a {@link java.lang.String} object.
     */
    public static String encodeBASE64(final byte[] bytes) {
        return new String(Base64.encodeBase64String(bytes));
    }

    /**
     * BASE64加密
     * 
     * @param str
     *            a {@link java.lang.String} object.
     * @param charset
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String encodeBASE64(final String str, String charset) {
        if (str == null) {
            return null;
        }
        try {
            byte[] bytes = str.getBytes(charset);
            return encodeBASE64(bytes);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * BASE64加密,默认UTF-8
     * 
     * @param str
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String encodeBASE64(final String str) {
        return encodeBASE64(str, DEFAULT_CHARSET);
    }

    /**
     * BASE64解密,默认UTF-8
     * 
     * @param str
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String decodeBASE64(String str) {
        return decodeBASE64(str, DEFAULT_CHARSET);
    }

    /**
     * BASE64解密
     * 
     * @param str
     *            a {@link java.lang.String} object.
     * @param charset
     *            字符编码
     * @return a {@link java.lang.String} object.
     */
    public static String decodeBASE64(String str, String charset) {
        try {
            byte[] bytes = str.getBytes(charset);
            return new String(Base64.decodeBase64(bytes));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * CRC32字节校验
     * 
     * @param bytes
     *            an array of byte.
     * @return a {@link java.lang.String} object.
     */
    public static String crc32(byte[] bytes) {
        CRC32 crc32 = new CRC32();
        crc32.update(bytes);
        return Long.toHexString(crc32.getValue());
    }

    /**
     * CRC32字符串校验
     * 
     * @param str
     *            a {@link java.lang.String} object.
     * @param charset
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String crc32(final String str, String charset) {
        try {
            byte[] bytes = str.getBytes(charset);
            return crc32(bytes);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * CRC32字符串校验,默认UTF-8编码读取
     * 
     * @param str
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String crc32(final String str) {
        return crc32(str, DEFAULT_CHARSET);
    }

    /**
     * CRC32流校验
     * 
     * @param input
     *            a {@link java.io.InputStream} object.
     * @return a {@link java.lang.String} object.
     */
    public static String crc32(InputStream input) {
        CRC32 crc32 = new CRC32();
        CheckedInputStream checkInputStream = null;
        int test = 0;
        try {
            checkInputStream = new CheckedInputStream(input, crc32);
            do {
                test = checkInputStream.read();
            } while (test != -1);
            return Long.toHexString(crc32.getValue());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * CRC32文件唯一校验
     * 
     * @param file
     *            a {@link java.io.File} object.
     * @return a {@link java.lang.String} object.
     */
    public static String crc32(File file) {
        InputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(file));
            return crc32(input);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    /**
     * CRC32文件唯一校验
     * 
     * @param url
     *            a {@link java.net.URL} object.
     * @return a {@link java.lang.String} object.
     */
    public static String crc32(URL url) {
        InputStream input = null;
        try {
            input = url.openStream();
            return crc32(input);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }
   
    /**
     * AES 加密操作
     *
     * @param content 待加密内容
     * @param password 加密密码
     * @return 返回Base64转码后的加密数据
     */
    public static String encryptAES(String content, String password) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);// 创建密码器

            byte[] byteContent = content.getBytes("utf-8");

            cipher.init(Cipher.ENCRYPT_MODE, getAESSecretKey(password));// 初始化为加密模式的密码器

            byte[] result = cipher.doFinal(byteContent);// 加密

            return Base64.encodeBase64String(result);//通过Base64转码返回
        } catch (Exception ex) {
            Logger.getLogger(EncryptUtil.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
    
    public static void main(String[] args) {
		String payPassword=encryptAES("888888", "ysgj");
		System.out.println("加密内容："+payPassword);
		String jiemi=decryptAES("ntTaNLwSVN9zIBC0GSHrR0bH2xMVdvJVf21sxmMVduIdkNZ5JBfCsAR0KOxoGtUD","ysgj");
		System.out.println("解密内容："+jiemi);
		BigDecimal a=BigDecimal.valueOf(1);
		BigDecimal b=BigDecimal.valueOf(0);
		System.out.println(a.compareTo(b));
    }

    /**
     * AES 解密操作
     *
     * @param content
     * @param password
     * @return
     */
    public static String decryptAES(String content, String password) {

        try {
            //实例化
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);

            //使用密钥初始化，设置为解密模式
            cipher.init(Cipher.DECRYPT_MODE, getAESSecretKey(password));

            //执行操作
            byte[] result = cipher.doFinal(Base64.decodeBase64(content));

            return new String(result, "utf-8");
        } catch (Exception ex) {
            Logger.getLogger(EncryptUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }

    /**
     * 生成加密秘钥
     *
     * @return
     */
    private static SecretKeySpec getAESSecretKey(final String password) {
//        //返回生成指定算法密钥生成器的 KeyGenerator 对象
//        KeyGenerator kg = null;
//
//        try {
//            kg = KeyGenerator.getInstance(KEY_ALGORITHM);
//
//
//            //AES 要求密钥长度为 128
////            kg.init(128, new SecureRandom(password.getBytes()));//这是window平台专用的，改成下面几句，兼容linux平台
//
//            //兼容linux ....
//            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
//            random.setSeed(password.getBytes());
//            kg.init(128,random);
//            //兼容linux end
//
//            //生成一个密钥
//            SecretKey secretKey = kg.generateKey();
//
//            return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);// 转换为AES专用密钥
//        } catch (NoSuchAlgorithmException ex) {
//            Logger.getLogger(EncryptUtil.class.getName()).log(Level.SEVERE, null, ex);
//        }
        String md5 = encodeMD5(password).substring(8, 24);//16位md5
        try {
            return new SecretKeySpec(md5.getBytes(DEFAULT_CHARSET), KEY_ALGORITHM);// 转换为AES专用密钥
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

}