package com.example.tg_bot.telegram;

import com.example.tg_bot.access.UserAccessChecker;
import com.example.tg_bot.telegram.command.CommandRouter;
import com.example.tg_bot.telegram.homework.HomeworkCallbackHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpdateDispatcherGuardsTest {
    private final UserAccessChecker access = mock(UserAccessChecker.class);
    private final CommandRouter commands = mock(CommandRouter.class);
    private final HomeworkCallbackHandler buttons = mock(HomeworkCallbackHandler.class);
    private final UpdateDispatcher dispatcher = new UpdateDispatcher(commands, buttons, access);

    @Test
    void ignoresNullUpdate() {
        assertDoesNotThrow(() -> dispatcher.dispatch(null));
        verifyNoInteractions(access, commands, buttons);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sender", "message", "data"})
    void ignoresCallbackMissingRequiredData(String missing) {
        when(access.isAllowed(1L)).thenReturn(true);
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(10L);
        when(message.getMessageId()).thenReturn(101);
        CallbackQuery query = new CallbackQuery();
        query.setId("press");
        query.setFrom(missing.equals("sender") ? null : new User(1L, "User", false));
        query.setMessage(missing.equals("message") ? null : message);
        query.setData(missing.equals("data") ? null : "NEXT_PAGE");
        Update update = new Update();
        update.setCallbackQuery(query);

        assertDoesNotThrow(() -> dispatcher.dispatch(update));

        verifyNoInteractions(commands, buttons);
        if (missing.equals("sender")) {
            verifyNoInteractions(access);
        }
    }

    @Test
    void continuesProcessingTheBatchAfterOneUpdateFails() {
        UpdateDispatcher failingDispatcher = mock(UpdateDispatcher.class);
        TelegramBot bot = new TelegramBot("test-token", failingDispatcher);
        Update first = new Update();
        first.setUpdateId(1);
        Update second = new Update();
        second.setUpdateId(2);
        doThrow(new IllegalStateException("Handler failed")).when(failingDispatcher).dispatch(first);

        assertDoesNotThrow(() -> bot.consume(List.of(first, second)));

        InOrder order = inOrder(failingDispatcher);
        order.verify(failingDispatcher).dispatch(first);
        order.verify(failingDispatcher).dispatch(second);
    }

    @Test
    void nullEntryInBatchDoesNotPreventProcessingTheFollowingCommand() {
        when(access.isAllowed(1L)).thenReturn(true);
        Message message = mock(Message.class);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/help");
        when(message.getFrom()).thenReturn(new User(1L, "User", false));
        when(message.getChatId()).thenReturn(10L);
        when(message.getMessageId()).thenReturn(100);
        Update update = new Update();
        update.setMessage(message);

        new TelegramBot("test-token", dispatcher).consume(Arrays.asList(null, update));

        verify(commands).route(1L, 10L, 100, "/help");
    }
}
