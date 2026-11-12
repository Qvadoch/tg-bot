package com.example.tg_bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Создаёт общие зависимости бота: часы и клиент Telegram API.
 */
@Configuration
public class TelegramConfig {

    /**
     * Создаёт источник текущего времени с настроенным часовым поясом.
     * Передача часов зависимым компонентам позволяет подменять время в тестах.
     *
     * @param zoneId часовой пояс из {@code bot.timezone}
     * @return системные часы с указанным часовым поясом
     */
    @Bean
    public Clock botClock(@Value("${bot.timezone}") ZoneId zoneId) {
        return Clock.system(zoneId);
    }

    /**
     * Создаёт клиент, выполняющий запросы от имени бота.
     *
     * @param tokenBot секрет из настройки {@code bot.token}, получаемый из внешней конфигурации
     * @return клиент Telegram API на основе OkHttp
     */
    @Bean
    public TelegramClient telegramClient(@Value("${bot.token}") String tokenBot) {
        return new OkHttpTelegramClient(tokenBot);
    }
}
