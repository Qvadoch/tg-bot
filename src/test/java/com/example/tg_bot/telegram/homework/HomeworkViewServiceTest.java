package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.homework.formatter.HomeworkFormatter;
import com.example.tg_bot.homework.model.HomeworkItem;
import com.example.tg_bot.telegram.TelegramSender;
import com.example.tg_bot.telegram.TelegramTextSplitter;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HomeworkViewServiceTest {
    private final TelegramClient client = mock(TelegramClient.class);
    private final AtomicReference<String> visibleText = new AtomicReference<>();

    @Test
    void reconcilesAnEditAppliedByTelegramWhenItsResponseWasLost() throws TelegramApiException {
        HomeworkViewService views = createService();
        AtomicBoolean responseLost = new AtomicBoolean();
        when(client.execute(any(EditMessageText.class))).thenAnswer(call -> {
            EditMessageText request = call.getArgument(0);
            if (request.getText().equals(visibleText.get())) {
                ApiResponse<Boolean> response = ApiResponse.<Boolean>builder().errorCode(400)
                        .errorDescription("Bad Request: message is not modified").build();
                throw new TelegramApiRequestException("Edit failed", response);
            }
            visibleText.set(request.getText());
            if (!responseLost.getAndSet(true)) {
                throw new TelegramApiException("Response lost", new IOException("Connection closed after apply"));
            }
            return Boolean.TRUE;
        });
        views.show(1, 10, 100, items(25));

        assertThrows(TelegramApiException.class, () -> views.turnPage(1, 10, 101, PageAction.NEXT_PAGE));
        assertTrue(visibleText.get().contains("Работа 11\n"));
        views.turnPage(1, 10, 101, PageAction.NEXT_PAGE);
        views.turnPage(1, 10, 101, PageAction.NEXT_PAGE);

        assertTrue(visibleText.get().contains("Работа 21\n"));
        verify(client, times(3)).execute(any(EditMessageText.class));
    }

    @Test
    void traversesMessagePartsAndHomeworkPagesInBothDirections() throws TelegramApiException {
        HomeworkViewService views = createService();
        when(client.execute(any(EditMessageText.class))).thenAnswer(call -> {
            visibleText.set(call.<EditMessageText>getArgument(0).getText());
            return Boolean.TRUE;
        });
        List<HomeworkItem> homework = new ArrayList<>(items(11));
        homework.set(0, new HomeworkItem("Java", "x".repeat(4500) + "📗", "https://example.com/1", null));
        views.show(1, 10, 100, homework);
        String firstPart = visibleText.get();
        views.turnPage(1, 10, 101, PageAction.NEXT_PAGE);
        String secondPart = visibleText.get();
        views.turnPage(1, 10, 101, PageAction.NEXT_PAGE);
        assertTrue(visibleText.get().contains("Работа 11\n"));
        views.turnPage(1, 10, 101, PageAction.NEXT_PAGE);
        verify(client, times(2)).execute(any(EditMessageText.class));

        views.turnPage(1, 10, 101, PageAction.PREVIOUS_PAGE);
        assertEquals(secondPart, visibleText.get());
        views.turnPage(1, 10, 101, PageAction.PREVIOUS_PAGE);
        assertEquals(firstPart, visibleText.get());
        views.turnPage(1, 10, 101, PageAction.PREVIOUS_PAGE);
        verify(client, times(4)).execute(any(EditMessageText.class));
    }

    private HomeworkViewService createService() throws TelegramApiException {
        Message reply = new Message();
        reply.setMessageId(101);
        when(client.execute(any(SendMessage.class))).thenAnswer(call -> {
            visibleText.set(call.<SendMessage>getArgument(0).getText());
            return reply;
        });
        Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        TelegramTextSplitter splitter = new TelegramTextSplitter();
        HomeworkPageRenderer renderer = new HomeworkPageRenderer(new HomeworkFormatter(), new HomeworkKeyboard(), clock, splitter);
        return new HomeworkViewService(new HomeworkSessionStore(clock, 1000, Duration.ofMinutes(30)),
                new TelegramSender(client, splitter), renderer);
    }

    private List<HomeworkItem> items(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> new HomeworkItem("Java", "Работа " + index, "https://example.com/" + index, null))
                .toList();
    }
}
