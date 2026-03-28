package com.smartroomfinder.smartroomfinder.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;


@Slf4j
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int    IV_SIZE   = 16; // 128-bit IV
    private static final int    KEY_SIZE  = 32; // 256-bit key

    private final byte[] keyBytes;

    public EncryptionService(@Value("${app.encryption.key}") String secretKey) {
        byte[] raw = secretKey.getBytes(StandardCharsets.UTF_8);
        if (raw.length != KEY_SIZE) {
            throw new IllegalStateException(
                    "app.encryption.key phải đúng 32 ký tự UTF-8 (AES-256). " +
                            "Hiện tại: " + raw.length + " ký tự."
            );
        }
        this.keyBytes = raw;
    }

    public String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) return plainText;

        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new IvParameterSpec(iv));

        byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[IV_SIZE + cipherBytes.length];
        System.arraycopy(iv,          0, combined, 0,       IV_SIZE);
        System.arraycopy(cipherBytes, 0, combined, IV_SIZE, cipherBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public String decrypt(String cipherBase64) throws Exception {
        if (cipherBase64 == null || cipherBase64.isEmpty()) return cipherBase64;

        byte[] combined = Base64.getDecoder().decode(cipherBase64);

        if (combined.length <= IV_SIZE) {
            throw new IllegalArgumentException("Dữ liệu mã hóa quá ngắn.");
        }

        byte[] iv          = new byte[IV_SIZE];
        byte[] cipherBytes = new byte[combined.length - IV_SIZE];
        System.arraycopy(combined, 0,       iv,          0, IV_SIZE);
        System.arraycopy(combined, IV_SIZE, cipherBytes, 0, cipherBytes.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new IvParameterSpec(iv));

        return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
    }

    public String safeDecrypt(String input) {
        if (input == null || input.isBlank()) return null;
        try {
            return decrypt(input);
        } catch (Exception e) {
            log.debug("safeDecrypt: không giải mã được, trả về plaintext. prefix='{}'",
                    input.substring(0, Math.min(20, input.length())));
            return input;
        }
    }
}