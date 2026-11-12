package com.example.tg_bot.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TelegramSenderTest {
    private static final Long CHAT_ID = 1L;
    private static final String TEXT = "текст";

    @Mock
    TelegramClient telegramClient;
    TelegramSender telegramSender;
    @Captor
    ArgumentCaptor<SendMessage> captor;

    @BeforeEach
    void setUp() {
        telegramSender = new TelegramSender(telegramClient, new TelegramTextSplitter());
    }

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

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 429})
    void skipsFailureNoticeWhenTelegramDeniesAccessOrLimitsRequests(int code) {
        TelegramApiRequestException rejection = mock(TelegramApiRequestException.class);
        when(rejection.getErrorCode()).thenReturn(code);

        telegramSender.sendFailureNotice(CHAT_ID, TEXT, rejection);

        verifyNoInteractions(telegramClient);
    }

    @Test
    void attemptsAShortNoticeOnlyOnceWhenDeliveryAlsoFails() throws TelegramApiException {
        TelegramApiRequestException rejection = mock(TelegramApiRequestException.class);
        when(rejection.getErrorCode()).thenReturn(400);
        when(telegramClient.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("delivery failed"));

        telegramSender.sendFailureNotice(CHAT_ID, TEXT, rejection);

        verify(telegramClient).execute(captor.capture());
        assertEquals(String.valueOf(CHAT_ID), captor.getValue().getChatId());
        assertEquals(TEXT, captor.getValue().getText());
    }

    @Test
    void treatsAnAlreadyAppliedEditAsSuccess() throws TelegramApiException {
        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder().errorCode(400)
                .errorDescription("Bad Request: message is not modified").build();
        when(telegramClient.execute(any(EditMessageText.class)))
                .thenThrow(new TelegramApiRequestException("Edit failed", response));

        assertDoesNotThrow(() -> telegramSender.editMessage(CHAT_ID, 1, TEXT, null));
    }

    @Test
    void propagatesOtherEditFailures() throws TelegramApiException {
        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder().errorCode(400)
                .errorDescription("Bad Request: message to edit not found").build();
        TelegramApiRequestException failure = new TelegramApiRequestException("Edit failed", response);
        when(telegramClient.execute(any(EditMessageText.class))).thenThrow(failure);

        assertSame(failure, assertThrows(TelegramApiRequestException.class,
                () -> telegramSender.editMessage(CHAT_ID, 1, TEXT, null)));
    }

    @Test
    void splitsLongTextAndPreservesAllCourseNames() throws TelegramApiException {
        String text = "Твои курсы:\n" + "Очень длинное название курса\n".repeat(400);

        telegramSender.sendText(CHAT_ID, text);

        verify(telegramClient, times(3)).execute(captor.capture());
        assertEquals(text, captor.getAllValues().stream().map(SendMessage::getText)
                .collect(java.util.stream.Collectors.joining()));
        assertTrue(captor.getAllValues().stream().allMatch(message -> message.getText().length() <= 4096));
    }

    @Test
    void stopsSendingPartsAfterTheFirstFailure() throws TelegramApiException {
        when(telegramClient.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("delivery failed"));

        telegramSender.sendText(CHAT_ID, "text".repeat(3000));

        verify(telegramClient).execute(any(SendMessage.class));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL|message is not modified",
            "400|NULL",
            "403|message is not modified",
            "429|message is not modified",
            "500|message is not modified"
    }, delimiter = '|', nullValues = "NULL")
    void onlyTheSpecificBadRequestConfirmsAnAlreadyAppliedEdit(Integer code, String description)
            throws TelegramApiException {
        TelegramApiRequestException failure = mock(TelegramApiRequestException.class);
        when(failure.getErrorCode()).thenReturn(code);
        when(failure.getApiResponse()).thenReturn(description);
        when(telegramClient.execute(any(EditMessageText.class))).thenThrow(failure);

        assertSame(failure, assertThrows(TelegramApiRequestException.class,
                () -> telegramSender.editMessage(CHAT_ID, 101, TEXT, null)));
        verify(telegramClient).execute(any(EditMessageText.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {400, 500})
    void attemptsANoticeForOtherRequestErrors(Integer code) throws TelegramApiException {
        TelegramApiRequestException failure = mock(TelegramApiRequestException.class);
        when(failure.getErrorCode()).thenReturn(code);

        telegramSender.sendFailureNotice(CHAT_ID, TEXT, failure);

        verify(telegramClient).execute(captor.capture());
        assertEquals(String.valueOf(CHAT_ID), captor.getValue().getChatId());
        assertEquals(TEXT, captor.getValue().getText());
    }

    @Test
    void attemptsANoticeAfterATransportFailure() throws TelegramApiException {
        telegramSender.sendFailureNotice(CHAT_ID, TEXT, new TelegramApiException("Connection lost"));

        verify(telegramClient).execute(captor.capture());
        assertEquals(TEXT, captor.getValue().getText());
    }

    @Test
    void doesNotSendTheThirdPartWhenTheSecondPartFails() throws TelegramApiException {
        when(telegramClient.execute(any(SendMessage.class)))
                .thenReturn(new Message())
                .thenThrow(new TelegramApiException("Second part failed"));

        assertDoesNotThrow(() -> telegramSender.sendText(CHAT_ID, "x".repeat(9000)));

        verify(telegramClient, times(2)).execute(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(message -> message.getText().length() == 4096));
        assertTrue(captor.getAllValues().stream()
                .allMatch(message -> String.valueOf(CHAT_ID).equals(message.getChatId())));
    }
}
