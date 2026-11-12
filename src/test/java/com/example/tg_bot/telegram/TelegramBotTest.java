package com.example.tg_bot.telegram;

import com.example.tg_bot.access.UserAccessChecker;
import com.example.tg_bot.telegram.homework.HomeworkCallbackHandler;
import com.example.tg_bot.telegram.command.CommandRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
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
class TelegramBotTest {
    private static final Long USER_ID = 1L;
    private static final Long CHAT_ID = 10L;
    private static final Integer COMMAND_MESSAGE_ID = 100;
    private static final String BOT_TOKEN = "fghdADASasbjwqADeiuywf132";

    @Mock
    UserAccessChecker userAccessChecker;
    @Mock
    CommandRouter commandRouter;
    @Mock
    HomeworkCallbackHandler callbackHandler;

    TelegramBot telegramBot;

    @BeforeEach
    void setUp() {
        UpdateDispatcher updateDispatcher = new UpdateDispatcher(commandRouter, callbackHandler, userAccessChecker);
        telegramBot = new TelegramBot(BOT_TOKEN, updateDispatcher);
    }

    @Test
    @DisplayName("getBotToken -> возвращает токен из конструктора")
    void returnsBotToken() {
        assertEquals(BOT_TOKEN, telegramBot.getBotToken());
    }

    @Test
    @DisplayName("getUpdatesConsumer -> возвращает самого бота")
    void returnsItselfAsUpdatesConsumer() {
        assertSame(telegramBot, telegramBot.getUpdatesConsumer());
    }

    @Test
    @DisplayName("разрешённый пользователь пишет /help -> роутер получает /help")
    void routesCommandForAllowedUser() {
        when(userAccessChecker.isAllowed(USER_ID)).thenReturn(true);
        Update update = updateWith(USER_ID);
        when(update.getMessage().getText()).thenReturn("/help");
        when(update.getMessage().getMessageId()).thenReturn(COMMAND_MESSAGE_ID);

        telegramBot.consume(List.of(update));

        verify(commandRouter).route(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID, "/help");
    }

    @ParameterizedTest(name = "{0} -> роутер получает /help")
    @ValueSource(strings = {"/help", "/help@mybot", " /help текст ", "/help\nтекст", "/help\tтекст"})
    @DisplayName("команда с @ботом или текстом после неё -> роутер получает чистую команду")
    void extractsCommandFromText(String text) {
        when(userAccessChecker.isAllowed(USER_ID)).thenReturn(true);
        Update update = updateWith(USER_ID);
        when(update.getMessage().getText()).thenReturn(text);
        when(update.getMessage().getMessageId()).thenReturn(COMMAND_MESSAGE_ID);

        telegramBot.consume(List.of(update));

        verify(commandRouter).route(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID, "/help");
    }

    @Test
    @DisplayName("запрещённый пользователь -> роутер не вызывается")
    void ignoresForbiddenUser() {
        when(userAccessChecker.isAllowed(USER_ID)).thenReturn(false);
        Update update = updateWith(USER_ID);

        telegramBot.consume(List.of(update));

        verifyNoInteractions(commandRouter);
    }

    @Test
    @DisplayName("сообщение без текста -> доступ не проверяется, роутер не вызывается")
    void ignoresMessageWithoutText() {
        Update update = updateWithoutText();

        telegramBot.consume(List.of(update));

        verifyNoInteractions(userAccessChecker);
        verifyNoInteractions(commandRouter);
    }

    @Test
    @DisplayName("обновление без сообщения -> доступ не проверяется, роутер не вызывается")
    void ignoresUpdateWithoutMessage() {
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(false);

        telegramBot.consume(List.of(update));

        verifyNoInteractions(userAccessChecker);
        verifyNoInteractions(commandRouter);
    }

    @Test
    @DisplayName("сообщение без отправителя (канал) -> не падает, роутер не вызывается")
    void ignoresMessageWithoutFrom() {
        Update update = updateWithoutFrom();

        assertDoesNotThrow(() -> telegramBot.consume(List.of(update)));

        verifyNoInteractions(userAccessChecker);
        verifyNoInteractions(commandRouter);
    }

    @Test
    @DisplayName("кнопка «Следующая» передаёт данные нажатия в HomeworkCallbackHandler")
    void routesPaginationButtonToCallbackHandler() {
        when(userAccessChecker.isAllowed(USER_ID)).thenReturn(true);
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(CHAT_ID);
        when(message.getMessageId()).thenReturn(101);

        telegramBot.consume(List.of(updateWithNextButton(message)));

        verify(callbackHandler).handle("press-1", USER_ID, CHAT_ID, 101, "NEXT_PAGE");
        verifyNoInteractions(commandRouter);
    }

    @Test
    @DisplayName("нажатие кнопки пользователем без доступа пропускается")
    void ignoresButtonFromForbiddenUser() {
        when(userAccessChecker.isAllowed(USER_ID)).thenReturn(false);

        telegramBot.consume(List.of(updateWithNextButton(mock(Message.class))));

        verify(userAccessChecker).isAllowed(USER_ID);
        verifyNoInteractions(callbackHandler, commandRouter);
    }

    private Update updateWithNextButton(Message message) {
        User user = new User(USER_ID, "Иван", false);
        CallbackQuery query = new CallbackQuery();
        query.setId("press-1");
        query.setFrom(user);
        query.setMessage(message);
        query.setData("NEXT_PAGE");
        Update update = new Update();
        update.setCallbackQuery(query);
        return update;
    }

    private Update updateWith(Long userId) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        User user = mock(User.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
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
        when(message.getChatId()).thenReturn(CHAT_ID);
        when(message.getFrom()).thenReturn(null);

        return update;
    }
}
