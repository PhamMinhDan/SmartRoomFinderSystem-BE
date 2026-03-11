package com.smartroomfinder.smartroomfinder.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Slf4j
@Service
public class EncryptionService {

    // Giữ nguyên key 16 ký tự giống code gốc để tương thích dữ liệu cũ
    @Value("${app.encryption.key:1234567890123456}")
    private String secretKey;

    public String encrypt(String plainText) throws Exception {
        SecretKeySpec keySpec = buildKey();
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decrypt(String encryptedBase64) throws Exception {
        SecretKeySpec keySpec = buildKey();
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decoded = Base64.getDecoder().decode(encryptedBase64);
        return new String(cipher.doFinal(decoded), "UTF-8");
    }

    public String safeDecrypt(String input) {
        if (input == null || input.isBlank()) return null;
        try {
            return decrypt(input);
        } catch (Exception e) {
            // Không decrypt được → coi như plaintext (dữ liệu cũ chưa mã hóa)
            log.debug("Cannot decrypt, treating as plaintext: {}", input.substring(0, Math.min(20, input.length())));
            return input;
        }
    }

    private SecretKeySpec buildKey() throws Exception {
        // Đảm bảo key đúng 16 bytes
        byte[] keyBytes = secretKey.substring(0, 16).getBytes("UTF-8");
        return new SecretKeySpec(keyBytes, "AES");
    }
}