package com.example.tg_bot.handler;

import com.example.tg_bot.bot.TelegramSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HelpHandlerTest {

    private static final Long CHAT_ID = 1L;

    @Mock
    TelegramSender telegramSender;

    @InjectMocks
    HelpHandler helpHandler;

    @Test
    @DisplayName("handle -> отправляет список команд")
    void handleSendsCommandsList() {
        helpHandler.handle(CHAT_ID);

        verify(telegramSender).sendText(CHAT_ID,
                "Команды:\n/start - начать\n/help - список команд\n" +
                        "/homework - показать домашние задания\n/courses - показать курсы");
    }

    @Test
    @DisplayName("command -> возвращает /help")
    void commandIsHelp() {
        assertEquals("/help", helpHandler.command());
    }
}