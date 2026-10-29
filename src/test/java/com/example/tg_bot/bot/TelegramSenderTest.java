package com.example.tg_bot.bot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramSenderTest {
    private static final Long CHAT_ID = 1L;
    private static final String TEXT = "текст";

    @Mock
    TelegramClient telegramClient;
    @InjectMocks
    TelegramSender telegramSender;
    @Captor
    ArgumentCaptor<SendMessage> captor;

    @Test
    @DisplayName("sendText -> отправляет SendMessage с нужным chatId и текстом")
    void sendsMessageWithChatIdAndText() throws TelegramApiException {
        telegramSender.sendText(CHAT_ID, TEXT);

        verify(telegramClient).execute(captor.capture());
        assertEquals(String.valueOf(CHAT_ID), captor.getValue().getChatId());
        assertEquals(TEXT, captor.getValue().getText());
    }

    @Test
    @DisplayName("Telegram бросает исключение -> оно не вылетает наружу")
    void doesNotThrowWhenTelegramFails() throws TelegramApiException {
        when(telegramClient.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("boom"));
        assertDoesNotThrow(() -> telegramSender.sendText(CHAT_ID, TEXT));
    }
}