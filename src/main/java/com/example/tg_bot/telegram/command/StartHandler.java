package com.example.tg_bot.telegram.command;

import com.example.tg_bot.telegram.TelegramSender;
import org.springframework.stereotype.Component;

/**
 * Обрабатывает {@code /start}: знакомит пользователя с назначением бота и командой справки.
 */
@Component
public class StartHandler implements CommandHandler {
    private final TelegramSender telegramSender;

    public StartHandler(TelegramSender telegramSender) {
        this.telegramSender = telegramSender;
    }

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public void handle(Long userId, Long chatId, Integer commandMessageId) {
        telegramSender.sendText(chatId, "Привет! Я показываю домашние задания из Google Classroom.\nНапиши /help, чтобы увидеть список команд.");
    }
}
