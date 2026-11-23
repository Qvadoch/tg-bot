package com.example.tg_bot.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Данные Google-подключения одного Telegram-пользователя.
 * Поля токенов предназначены для значений, зашифрованных до сохранения.
 */
@Entity
@Table(name = "google_connections")
public class GoogleConnection {
    @Id
    @Column(name = "telegram_user_id", nullable = false)
    private Long telegramUserId;

    @Size(max = 255)
    @Column(name = "google_subject", length = 255)
    private String googleSubject;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "access_token_encrypted", nullable = false, columnDefinition = "text")
    private String accessTokenEncrypted;

    @Column(name = "refresh_token_encrypted", nullable = false, columnDefinition = "text")
    private String refreshTokenEncrypted;

    @Column(name = "access_token_expires_at", nullable = false)
    private Instant accessTokenExpiresAt;

    protected GoogleConnection() {
    }

    public GoogleConnection(Long telegramUserId, String googleSubject, String email, String accessTokenEncrypted, String refreshTokenEncrypted, Instant accessTokenExpiresAt) {
        this.telegramUserId = telegramUserId;
        this.googleSubject = googleSubject;
        this.email = email;
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.refreshTokenEncrypted = refreshTokenEncrypted;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public String getEmail() {
        return email;
    }

    public String getAccessTokenEncrypted() {
        return accessTokenEncrypted;
    }

    public String getRefreshTokenEncrypted() {
        return refreshTokenEncrypted;
    }

    public Instant getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public void setGoogleSubject(String googleSubject) {
        this.googleSubject = googleSubject;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAccessTokenEncrypted(String accessTokenEncrypted) {
        this.accessTokenEncrypted = accessTokenEncrypted;
    }

    public void setRefreshTokenEncrypted(String refreshTokenEncrypted) {
        this.refreshTokenEncrypted = refreshTokenEncrypted;
    }

    public void setAccessTokenExpiresAt(Instant accessTokenExpiresAt) {
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }
}
