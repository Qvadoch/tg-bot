package com.example.tg_bot.handler;

import com.example.tg_bot.bot.TelegramSender;
import org.springframework.stereotype.Component;

@Component
public class DefaultHandler {

    private final TelegramSender telegramSender;

    public DefaultHandler(TelegramSender telegramSender) {
        this.telegramSender = telegramSender;
    }

    public void handle(Long chatId) {
        telegramSender.sendText(chatId, "Такой команды нет. Напиши /help.");
    }
}