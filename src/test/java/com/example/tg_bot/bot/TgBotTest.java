package com.example.tg_bot.bot;

import com.example.tg_bot.access.AccessChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TgBotTest {
    private static final Long USER_ID = 1L;
    private static final Long CHAT_ID = 10L;
    private static final String BOT_TOKEN = "fghdADASasbjwqADeiuywf132";

    @Mock
    AccessChecker accessChecker;
    @Mock
    MessageRouter messageRouter;

    TgBot tgBot;

    @BeforeEach
    void setUp() {
        tgBot = new TgBot(BOT_TOKEN, accessChecker, messageRouter);
    }

    @Test
    @DisplayName("getBotToken -> возвращает токен из конструктора")
    void returnsBotToken() {
        assertEquals(BOT_TOKEN, tgBot.getBotToken());
    }

    @Test
    @DisplayName("getUpdatesConsumer -> возвращает самого бота")
    void returnsItselfAsUpdatesConsumer() {
        assertSame(tgBot, tgBot.getUpdatesConsumer());
    }

    @Test
    @DisplayName("разрешённый пользователь пишет /help -> роутер получает /help")
    void routesCommandForAllowedUser() {
        when(accessChecker.isAllowed(USER_ID)).thenReturn(true);
        Update update = updateWith("/help", USER_ID);

        tgBot.consume(List.of(update));

        verify(messageRouter).route(CHAT_ID, "/help");
    }

    @ParameterizedTest(name = "{0} -> роутер получает /help")
    @ValueSource(strings = {"/help", "/help@mybot", " /help текст "})
    @DisplayName("команда с @ботом или текстом после неё -> роутер получает чистую команду")
    void extractsCommandFromText(String text) {
        when(accessChecker.isAllowed(USER_ID)).thenReturn(true);
        Update update = updateWith(text, USER_ID);

        tgBot.consume(List.of(update));

        verify(messageRouter).route(CHAT_ID, "/help");
    }

    @Test
    @DisplayName("запрещённый пользователь -> роутер не вызывается")
    void ignoresForbiddenUser() {
        when(accessChecker.isAllowed(USER_ID)).thenReturn(false);
        Update update = updateWith("/help", USER_ID);

        tgBot.consume(List.of(update));

        verifyNoInteractions(messageRouter);
    }

    @Test
    @DisplayName("сообщение без текста -> доступ не проверяется, роутер не вызывается")
    void ignoresMessageWithoutText() {
        Update update = updateWithoutText();

        tgBot.consume(List.of(update));

        verifyNoInteractions(accessChecker);
        verifyNoInteractions(messageRouter);
    }

    @Test
    @DisplayName("обновление без сообщения -> доступ не проверяется, роутер не вызывается")
    void ignoresUpdateWithoutMessage() {
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(false);

        tgBot.consume(List.of(update));

        verifyNoInteractions(accessChecker);
        verifyNoInteractions(messageRouter);
    }

    @Test
    @DisplayName("сообщение без отправителя (канал) -> не падает, роутер не вызывается")
    void ignoresMessageWithoutFrom() {
        Update update = updateWithoutFrom();

        assertDoesNotThrow(() -> tgBot.consume(List.of(update)));

        verifyNoInteractions(accessChecker);
        verifyNoInteractions(messageRouter);
    }

    private Update updateWith(String text, Long userId) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        User user = mock(User.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(text);
        when(message.getChatId()).thenReturn(CHAT_ID);
        when(message.getFrom()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        return update;
    }

    private Update updateWithoutText() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(false);
        return update;
    }

    private Update updateWithoutFrom() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/help");
        when(message.getChatId()).thenReturn(CHAT_ID);
        when(message.getFrom()).thenReturn(null);

        return update;
    }
}