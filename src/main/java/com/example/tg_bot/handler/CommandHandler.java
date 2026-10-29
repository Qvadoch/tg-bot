package com.example.tg_bot.handler;

public interface CommandHandler {
    String command();
    void handle(Long chatId);
}
