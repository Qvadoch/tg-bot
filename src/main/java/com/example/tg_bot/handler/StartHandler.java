package com.example.tg_bot.handler;

import com.example.tg_bot.bot.TelegramSender;
import org.springframework.stereotype.Component;

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
    public void handle(Long chatId) {
        telegramSender.sendText(chatId, "Привет! Я показываю домашние задания из Google Classroom.\nНапиши /help, чтобы увидеть список команд.");
    }
}
