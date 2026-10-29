package com.example.tg_bot.handler;

import com.example.tg_bot.bot.TelegramSender;
import org.springframework.stereotype.Component;

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
    public void handle(Long chatId) {
        telegramSender.sendText(chatId, "Команды:\n/start - начать\n/help - список команд\n" +
                "/homework - показать домашние задания\n/courses - показать курсы");
    }
}
