package com.example.tg_bot.telegram.command;

import com.example.tg_bot.telegram.TelegramSender;
import org.springframework.stereotype.Component;

/**
 * Обрабатывает {@code /help} и отправляет список доступных команд.
 */
@Component
public class HelpHandler implements CommandHandler {

    private final TelegramSender telegramSender;

    public HelpHandler(TelegramSender telegramSender) {
        this.telegramSender = telegramSender;
    }

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public void handle(Long userId, Long chatId, Integer commandMessageId) {
        telegramSender.sendText(chatId, "Команды:\n/start - начать\n/help - список команд\n" +
                "/homework - показать домашние задания\n/courses - показать курсы");
    }
}
