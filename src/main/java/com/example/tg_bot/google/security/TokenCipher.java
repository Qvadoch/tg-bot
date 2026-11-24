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

/**
 * Шифрует и расшифровывает Google-токены с использованием AES-256-GCM.
 *
 * <p>Формат результата: Base64 от последовательности IV, шифротекста и тега GCM.
 * Ключ загружается из внешней настройки и не включается в результат шифрования.
 */
@Component
public class TokenCipher {
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    // Непустой токен занимает минимум один байт; остальные байты нужны для IV и тега GCM.
    private static final int MIN_PAYLOAD_LENGTH_BYTES = IV_LENGTH_BYTES + TAG_LENGTH_BITS / Byte.SIZE + 1;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Загружает и проверяет ключ AES-256 при создании бина.
     *
     * @param encodedKey Base64-представление ключа из 32 байтов из настройки google.tokens.encryption-key
     * @throws IllegalArgumentException если значение равно null, пустое, состоит из пробельных символов,
     *                                  имеет неверный Base64 или декодируется не в 32 байта
     */
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
     *
     * @param token исходный токен; не должен быть null, пустым или состоять из пробельных символов
     * @return Base64 от последовательности: 12 байтов IV, шифротекст UTF-8-токена и тег GCM
     * @throws IllegalArgumentException если токен равен null, пустой или состоит из пробельных символов
     * @throws IllegalStateException если криптографическая операция завершилась ошибкой
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

    /**
     * Восстанавливает токен из Base64-пакета, созданного методом {@link #encrypt(String)}.
     * Извлекает сохранённый IV и проверяет тег GCM при расшифровании.
     *
     * @param encryptedToken Base64-представление IV, шифротекста и тега GCM
     * @return исходный токен, восстановленный из байтов UTF-8
     * @throws IllegalArgumentException если значение равно null, пустое, состоит из пробельных символов,
     *                                  имеет неверный Base64 или пакет слишком короткий
     * @throws IllegalStateException если криптографическая операция завершилась ошибкой,
     *                               в том числе из-за неверного ключа или тега
     */
    public String decrypt(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.isBlank()) {
            throw new IllegalArgumentException("Encrypted token must not be blank");
        }

        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(encryptedToken);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Encrypted token must be valid Base64", exception);
        }

        if (payload.length < MIN_PAYLOAD_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "Encrypted token must contain at least " + MIN_PAYLOAD_LENGTH_BYTES + " bytes");
        }

        ByteBuffer buffer = ByteBuffer.wrap(payload);
        byte[] iv = new byte[IV_LENGTH_BYTES];
        buffer.get(iv);

        byte[] encryptedBytes = new byte[buffer.remaining()];
        buffer.get(encryptedBytes);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameters = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameters);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to decrypt Google token", exception);
        }
    }
}
