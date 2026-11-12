package com.example.tg_bot.telegram.command;

import com.example.tg_bot.telegram.TelegramSender;
import org.springframework.stereotype.Component;

/**
 * Отвечает на неизвестные команды предложением открыть справку.
 */
@Component
public class DefaultHandler {

    private final TelegramSender telegramSender;

    public DefaultHandler(TelegramSender telegramSender) {
        this.telegramSender = telegramSender;
    }

    /**
     * Отправляет подсказку о команде {@code /help}.
     *
     * @param chatId чат, в который нужно отправить ответ
     */
    public void handle(Long chatId) {
        telegramSender.sendText(chatId, "Такой команды нет. Напиши /help.");
    }
}
