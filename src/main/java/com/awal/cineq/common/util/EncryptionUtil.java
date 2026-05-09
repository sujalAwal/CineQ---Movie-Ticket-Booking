package com.awal.cineq.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Slf4j
@Component
public class EncryptionUtil {

    private final String encryptionKey;
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 16; // 128-bit key for AES

    public EncryptionUtil(@Value("${jwt.secret}") String jwtSecret) {
        // Derive a 128-bit key from JWT secret
        this.encryptionKey = jwtSecret.substring(0, Math.min(KEY_SIZE, jwtSecret.length()));
        if (this.encryptionKey.length() < KEY_SIZE) {
            this.encryptionKey.concat("0".repeat(KEY_SIZE - this.encryptionKey.length()));
        }
    }

    public String encrypt(String plainText) {
        try {
            if (plainText == null || plainText.isBlank()) {
                return plainText;
            }

            SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(), 0, KEY_SIZE, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Error encrypting value", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            if (encryptedText == null || encryptedText.isBlank()) {
                return encryptedText;
            }

            SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(), 0, KEY_SIZE, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decodedBytes);
            return new String(decrypted);
        } catch (Exception e) {
            log.error("Error decrypting value", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
