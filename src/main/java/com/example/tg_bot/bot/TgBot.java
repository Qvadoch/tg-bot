package com.example.tg_bot.bot;

import com.example.tg_bot.access.AccessChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;


@Component
public class TgBot implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(TgBot.class);

    private final String botToken;
    private final AccessChecker accessChecker;
    private final MessageRouter messageRouter;

    public TgBot(@Value("${bot.token}") String botToken, AccessChecker accessChecker, MessageRouter messageRouter) {
        this.botToken = botToken;
        this.accessChecker = accessChecker;
        this.messageRouter = messageRouter;
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Long chatId = update.getMessage().getChatId();
                String command = update.getMessage().getText().trim().split("[ @]")[0];
                User from = update.getMessage().getFrom();

                if (from == null) {
                    log.warn("Сообщение без отправителя (канал?), пропускаем");
                    continue;
                }

                Long userId = from.getId();

                if (!accessChecker.isAllowed(userId)) {
                    log.warn("Пользователь с ID: {} не имеет доступ к боту", userId);
                    continue;
                }

                messageRouter.route(chatId, command);
            }
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
