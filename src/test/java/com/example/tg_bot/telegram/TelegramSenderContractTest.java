package com.example.tg_bot.telegram;

import com.example.tg_bot.telegram.homework.HomeworkKeyboard;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TelegramSenderContractTest {
    private final TelegramClient client = mock(TelegramClient.class);
    private final TelegramSender sender = new TelegramSender(client, new TelegramTextSplitter());

    @Test
    void sendsTextAndKeyboardToRequestedChatAndReturnsTheActualMessageId() throws TelegramApiException {
        Message reply = new Message();
        reply.setMessageId(501);
        InlineKeyboardMarkup keyboard = new HomeworkKeyboard().getInlineKeyboardMarkup();
        when(client.execute(any(SendMessage.class))).thenReturn(reply);

        assertEquals(501, sender.sendMessage(10L, "Задания", keyboard));

        ArgumentCaptor<SendMessage> request = ArgumentCaptor.forClass(SendMessage.class);
        verify(client).execute(request.capture());
        assertEquals("10", request.getValue().getChatId());
        assertEquals("Задания", request.getValue().getText());
        assertSame(keyboard, request.getValue().getReplyMarkup());
    }

    @Test
    void propagatesFailedMainReplyWithoutRetryingIt() throws TelegramApiException {
        TelegramApiException failure = new TelegramApiException("Lost response");
        when(client.execute(any(SendMessage.class))).thenThrow(failure);
        assertSame(failure, assertThrows(TelegramApiException.class, () -> sender.sendMessage(10L, "Text", null)));
        verify(client, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void editsTheSpecifiedMessageAndCanRemoveItsKeyboard() throws TelegramApiException {
        sender.editMessage(10L, 501, "Обновлено", null);
        ArgumentCaptor<EditMessageText> request = ArgumentCaptor.forClass(EditMessageText.class);
        verify(client).execute(request.capture());
        assertEquals("10", request.getValue().getChatId());
        assertEquals(501, request.getValue().getMessageId());
        assertEquals("Обновлено", request.getValue().getText());
        assertNull(request.getValue().getReplyMarkup());
    }

    @Test
    void deletionTargetsTheSpecifiedMessageAndPropagatesFailure() throws TelegramApiException {
        TelegramApiException failure = new TelegramApiException("Delete failed");
        when(client.execute(any(DeleteMessage.class))).thenThrow(failure);
        assertSame(failure, assertThrows(TelegramApiException.class, () -> sender.deleteMessage(10L, 501)));
        ArgumentCaptor<DeleteMessage> request = ArgumentCaptor.forClass(DeleteMessage.class);
        verify(client).execute(request.capture());
        assertEquals("10", request.getValue().getChatId());
        assertEquals(501, request.getValue().getMessageId());
    }

    @Test
    void acknowledgmentFailureDoesNotEscapeOrTriggerARetry() throws TelegramApiException {
        when(client.execute(any(AnswerCallbackQuery.class))).thenThrow(new TelegramApiException("Ack failed"));
        assertDoesNotThrow(() -> sender.answerCallbackQuery("callback-1"));
        ArgumentCaptor<AnswerCallbackQuery> request = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(client).execute(request.capture());
        assertEquals("callback-1", request.getValue().getCallbackQueryId());
    }
}
