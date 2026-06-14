package com.coze.aicoze.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES加密工具类
 * 用于加密和解密传输数据
 */
@Component
public class AesUtil {

    @Value("${encrypt.aes-key}")
    private String aesKey;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * 获取密钥
     */
    private SecretKeySpec getSecretKeySpec() {
        // 确保密钥长度为16/24/32字节
        byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
        byte[] finalKey = new byte[32];
        System.arraycopy(keyBytes, 0, finalKey, 0, Math.min(keyBytes.length, 32));
        return new SecretKeySpec(finalKey, ALGORITHM);
    }

    /**
     * AES加密
     * @param plainText 明文
     * @return 加密后的Base64字符串
     */
    public String encrypt(String plainText) {
        try {
            if (plainText == null || plainText.isEmpty()) {
                return plainText;
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKeySpec());
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * AES解密
     * @param encryptedText 加密后的Base64字符串
     * @return 解密后的明文
     */
    public String decrypt(String encryptedText) {
        try {
            if (encryptedText == null || encryptedText.isEmpty()) {
                return encryptedText;
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKeySpec());
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }
}

