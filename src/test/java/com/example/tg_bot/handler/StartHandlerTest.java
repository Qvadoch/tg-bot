package com.example.tg_bot.handler;

import com.example.tg_bot.bot.TelegramSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StartHandlerTest {
    @Mock
    TelegramSender telegramSender;
    @InjectMocks
    StartHandler startHandler;
    private static final Long CHAT_ID = 1L;

    @Test
    @DisplayName("handle -> отправляет приветствие")
    void sendsGreeting() {
        startHandler.handle(CHAT_ID);

        verify(telegramSender).sendText(CHAT_ID, "Привет! Я показываю домашние задания из Google Classroom.\nНапиши /help, чтобы увидеть список команд.");
    }

    @Test
    @DisplayName("command -> возвращает /start")
    void commandIsStart() {
        assertEquals("/start", startHandler.command());
    }
}