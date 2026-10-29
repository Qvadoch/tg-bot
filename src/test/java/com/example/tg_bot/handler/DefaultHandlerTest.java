package com.example.tg_bot.handler;

import com.example.tg_bot.bot.TelegramSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultHandlerTest {

    private static final Long CHAT_ID = 1L;

    @Mock
    TelegramSender telegramSender;

    @InjectMocks
    DefaultHandler defaultHandler;

    @Test
    @DisplayName("handle -> отправляет сообщение 'нет команды'")
    void handleSendsUnknownCommandMessage() {
        defaultHandler.handle(CHAT_ID);

        verify(telegramSender).sendText(CHAT_ID, "Такой команды нет. Напиши /help.");
    }
}