
package com.example.tg_bot.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

/**
 * Подключает приложение к long polling Telegram и передаёт обновления диспетчеру.
 * Обновления одной полученной пачки обрабатываются последовательно.
 */
@Component
public class TelegramBot implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    private final String botToken;
    private final UpdateDispatcher updateDispatcher;

    public TelegramBot(@Value("${bot.token}") String botToken, UpdateDispatcher updateDispatcher) {
        this.botToken = botToken;
        this.updateDispatcher = updateDispatcher;
    }

    /**
     * Обрабатывает пачку обновлений в заданном порядке.
     * Ошибка выполнения одного обработчика записывается в журнал, после чего обработка продолжается.
     *
     * @param updates полученная пачка обновлений
     */
    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            try {
                updateDispatcher.dispatch(update);
            } catch (RuntimeException exception) {
                log.error("Failed to process Telegram update {}", update.getUpdateId(), exception);
            }
        }
    }

    /**
     * Возвращает токен для регистрации бота в механизме long polling.
     *
     * @return секрет из внешней настройки {@code bot.token}
     */
    @Override
    public String getBotToken() {
        return botToken;
    }

    /**
     * Предоставляет обработчик обновлений библиотеке TelegramBots.
     *
     * @return этот объект как потребитель обновлений
     */
    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }
}
