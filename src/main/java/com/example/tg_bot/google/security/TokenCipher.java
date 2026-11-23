package com.example.tg_bot.google.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/** Шифрует токены для хранения в БД; расшифрование будет добавлено следующим этапом. */
@Component
public class TokenCipher {
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    /** Загружает ключ AES-256 из Base64-настройки и проверяет её при создании бина. */
    public TokenCipher(@Value("${google.tokens.encryption-key}") String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalArgumentException("google.tokens.encryption-key must not be blank");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("google.tokens.encryption-key must be valid Base64", exception);
        }

        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("google.tokens.encryption-key must decode to 32 bytes for AES-256");
        }

        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Шифрует токен с новым случайным IV.
     * Возвращает Base64 от последовательности: 12 байтов IV, шифротекст и тег GCM.
     */
    public String encrypt(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be blank");
        }

        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameters = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameters);

            byte[] encryptedBytes = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + encryptedBytes.length)
                    .put(iv)
                    .put(encryptedBytes)
                    .array();

            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt Google token", exception);
        }
    }

    /** Получает зашифрованное значение и должен восстанавливать исходный токен. */
    public String decrypt(String encryptedToken) {
        throw new UnsupportedOperationException("Расшифрование токена пока не реализовано");
    }
}
