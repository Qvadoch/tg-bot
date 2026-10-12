package com.example.tg_bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;


@Component
public class TgBot implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private final String botToken;
    private final TelegramClient telegramClient;

    public TgBot(@Value("${bot.token}") String botToken) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }


    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Long chatId = update.getMessage().getChatId();

                switch (update.getMessage().getText()) {
                    case "/start":
                        sendText(chatId, "Привет! Я показываю домашние задания из Google Classroom.\nНапиши /help, чтобы увидеть список команд.");
                        break;
                    case "/help":
                        sendText(chatId, "Команды:\n/start - начать\n/help - список команд\n/homework - показать домашние задания");
                        break;
                    case "/homework":
                        sendText(chatId, "Эта функция пока в разработке.");
                        break;
                    default:
                        sendText(chatId, "Такой команды нет. Напиши /help.");
                        break;
                }
            }
        }
    }

    private void sendText(Long chatId, String text) {
        SendMessage sendMessage = SendMessage.builder().chatId(chatId).text(text).build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }
}
