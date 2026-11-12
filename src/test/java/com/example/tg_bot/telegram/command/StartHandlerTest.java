package com.example.tg_bot.telegram.command;

import com.example.tg_bot.telegram.TelegramSender;
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
    private static final Long USER_ID = 10L;
    private static final Long CHAT_ID = 1L;
    private static final Integer COMMAND_MESSAGE_ID = 100;

    @Test
    @DisplayName("handle -> отправляет приветствие")
    void sendsGreeting() {
        startHandler.handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);

        verify(telegramSender).sendText(CHAT_ID, "Привет! Я показываю домашние задания из Google Classroom.\nНапиши /help, чтобы увидеть список команд.");
    }

    @Test
    @DisplayName("command -> возвращает /start")
    void commandIsStart() {
        assertEquals("/start", startHandler.command());
    }
}
