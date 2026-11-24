package com.example.tg_bot.google.service;

import com.example.tg_bot.db.model.GoogleConnection;
import com.example.tg_bot.db.repository.GoogleConnectionRepository;
import com.example.tg_bot.google.model.GoogleTokens;
import com.example.tg_bot.google.security.TokenCipher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Управляет сохранёнными Google-подключениями по Telegram ID пользователя.
 * Перед записью шифрует исходные токены, а для запросов к Google возвращает
 * расшифрованные значения отдельно от сущности БД.
 */
@Service
public class GoogleConnectionService {
    private final GoogleConnectionRepository googleConnectionRepository;
    private final TokenCipher tokenCipher;

    public GoogleConnectionService(GoogleConnectionRepository googleConnectionRepository, TokenCipher tokenCipher) {
        this.googleConnectionRepository = googleConnectionRepository;
        this.tokenCipher = tokenCipher;
    }

    /** Проверяет наличие записи, а не действительность разрешений Google. */
    public boolean hasConnection(long telegramUserId) {
        return googleConnectionRepository.existsById(telegramUserId);
    }

    /** Возвращает сущность с зашифрованными токенами либо пустой Optional, если записи нет. */
    public Optional<GoogleConnection> findByTelegramUserId(long telegramUserId) {
        return googleConnectionRepository.findById(telegramUserId);
    }

    /**
     * Сохраняет полное подключение, шифруя оба токена перед созданием сущности.
     * При существующем Telegram ID заменяет сохранённые данные подключения.
     * Вызывающий код передаёт идентификатор и почту из проверенного ответа Google.
     *
     * @param telegramUserId Telegram ID владельца подключения
     * @param googleSubject постоянный идентификатор Google-аккаунта (sub)
     * @param email почта Google-аккаунта
     * @param accessToken исходный access token
     * @param refreshToken исходный refresh token; для этого метода обязателен
     * @param accessTokenExpiresAt момент истечения access token
     * @return сохранённая сущность с зашифрованными токенами
     * @throws IllegalArgumentException если обязательные данные отсутствуют или токены пустые
     * @throws IllegalStateException если шифрование завершилось ошибкой
     */
    public GoogleConnection save(long telegramUserId, String googleSubject, String email,
                                 String accessToken, String refreshToken, Instant accessTokenExpiresAt) {
        if (googleSubject == null || googleSubject.isBlank()) {
            throw new IllegalArgumentException("Google subject must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google email must not be blank");
        }
        if (accessTokenExpiresAt == null) {
            throw new IllegalArgumentException("Access token expiration must not be null");
        }

        String encryptedAccessToken = tokenCipher.encrypt(accessToken);
        String encryptedRefreshToken = tokenCipher.encrypt(refreshToken);
        GoogleConnection connection = new GoogleConnection(telegramUserId, googleSubject, email,
                encryptedAccessToken, encryptedRefreshToken, accessTokenExpiresAt);
        return googleConnectionRepository.save(connection);
    }

    /**
     * Читает и расшифровывает оба токена для обращения к Google.
     * Сохранённая сущность остаётся с зашифрованными значениями.
     *
     * @param telegramUserId Telegram ID владельца подключения
     * @return исходные токены и срок действия access token либо пустой Optional, если записи нет
     * @throws IllegalArgumentException если сохранённые значения имеют неверный формат
     * @throws IllegalStateException если расшифрование или проверка тега завершились ошибкой
     */
    public Optional<GoogleTokens> findTokensByTelegramUserId(long telegramUserId) {
        Optional<GoogleConnection> optionalConnection = googleConnectionRepository.findById(telegramUserId);
        if (optionalConnection.isEmpty()) {
            return Optional.empty();
        }

        GoogleConnection connection = optionalConnection.get();
        String accessToken = tokenCipher.decrypt(connection.getAccessTokenEncrypted());
        String refreshToken = tokenCipher.decrypt(connection.getRefreshTokenEncrypted());
        return Optional.of(new GoogleTokens(accessToken, refreshToken, connection.getAccessTokenExpiresAt()));
    }

    /** Удаляет запись подключения; отсутствие записи не считается ошибкой. */
    public void deleteByTelegramUserId(long telegramUserId) {
        googleConnectionRepository.deleteById(telegramUserId);
    }
}
