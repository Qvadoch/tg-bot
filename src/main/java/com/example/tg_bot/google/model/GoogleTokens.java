package com.example.tg_bot.google.model;

import java.time.Instant;

/**
 * Расшифрованные токены для обращения к Google и срок действия access token.
 * Используется в памяти приложения; представление toString скрывает значения токенов.
 *
 * @param accessToken исходный access token
 * @param refreshToken исходный refresh token
 * @param accessTokenExpiresAt момент истечения access token
 */
public record GoogleTokens(String accessToken, String refreshToken, Instant accessTokenExpiresAt) {
    @Override
    public String toString() {
        return "GoogleTokens[accessToken=<redacted>, refreshToken=<redacted>, accessTokenExpiresAt="
                + accessTokenExpiresAt + "]";
    }
}
