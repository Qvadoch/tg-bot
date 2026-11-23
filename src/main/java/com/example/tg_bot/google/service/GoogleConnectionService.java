package com.example.tg_bot.google.service;

import com.example.tg_bot.db.model.GoogleConnection;
import com.example.tg_bot.db.repository.GoogleConnectionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Управляет сохранёнными Google-подключениями по Telegram ID пользователя.
 * Токены в передаваемой сущности должны быть заранее зашифрованы.
 */
@Service
public class GoogleConnectionService {
    private final GoogleConnectionRepository googleConnectionRepository;

    public GoogleConnectionService(GoogleConnectionRepository googleConnectionRepository) {
        this.googleConnectionRepository = googleConnectionRepository;
    }

    /** Проверяет наличие записи, а не действительность разрешений Google. */
    public boolean hasConnection(long telegramUserId) {
        return googleConnectionRepository.existsById(telegramUserId);
    }

    /** Возвращает подключение либо пустой Optional, если пользователь ещё не подключился. */
    public Optional<GoogleConnection> findByTelegramUserId(long telegramUserId) {
        return googleConnectionRepository.findById(telegramUserId);
    }

    /** Сохраняет новое подключение или обновляет существующее с тем же Telegram ID. */
    public GoogleConnection save(GoogleConnection connection) {
        return googleConnectionRepository.save(connection);
    }

    /** Удаляет запись подключения; отсутствие записи не считается ошибкой. */
    public void deleteByTelegramUserId(long telegramUserId) {
        googleConnectionRepository.deleteById(telegramUserId);
    }
}
