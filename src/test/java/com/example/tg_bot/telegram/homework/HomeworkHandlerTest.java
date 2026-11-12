package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.classroom.exception.ClassroomIntegrationException;
import com.example.tg_bot.classroom.service.ClassroomService;
import com.example.tg_bot.homework.formatter.HomeworkFormatter;
import com.example.tg_bot.homework.model.HomeworkItem;
import com.example.tg_bot.telegram.TelegramSender;
import com.example.tg_bot.telegram.TelegramTextSplitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeworkHandlerTest {
    private static final Long USER = 10L;
    private static final Long CHAT = 1L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-25T09:00:00Z"), ZoneId.of("Europe/Moscow"));

    @Mock TelegramSender sender;
    @Mock ClassroomService classroom;
    private HomeworkHandler handler;
    private HomeworkCallbackHandler buttons;

    @BeforeEach
    void setUp() {
        HomeworkPageRenderer renderer = new HomeworkPageRenderer(new HomeworkFormatter(), new HomeworkKeyboard(), CLOCK,
                new TelegramTextSplitter());
        HomeworkSessionStore sessions = new HomeworkSessionStore(CLOCK, 1000, Duration.ofMinutes(30));
        HomeworkViewService views = new HomeworkViewService(sessions, sender, renderer);
        handler = new HomeworkHandler(classroom, views, sender);
        buttons = new HomeworkCallbackHandler(sender, views);
    }

    @Test
    void showsFirstPageThenEditsOnlyTheActiveReply() throws TelegramApiException {
        when(classroom.getHomework()).thenReturn(items("Старое", 25), items("Новое", 1));
        when(sender.sendMessage(eq(CHAT), anyString(), any())).thenReturn(101, 201);

        handler.handle(USER, CHAT, 100);
        verify(sender).sendMessage(eq(CHAT), argThat(s -> s.startsWith("Задания (25):") &&
                s.contains("Старое 10\n") && !s.contains("Старое 11\n") && s.contains("Срок: завтра в 14:30")), any());
        verify(sender, never()).deleteMessage(anyLong(), anyInt());
        press(USER, CHAT, 101, "NEXT_PAGE");
        verify(sender).editMessage(eq(CHAT), eq(101), contains("Старое 11\n"), any());

        handler.handle(USER, CHAT, 200);
        press(USER, CHAT, 101, "NEXT_PAGE");
        press(USER, CHAT, 201, "PREVIOUS_PAGE");
        verify(sender, times(1)).editMessage(anyLong(), anyInt(), anyString(), any());
        verify(sender, times(3)).answerCallbackQuery("press");
    }

    @Test
    void failedSendKeepsThePreviousSnapshotAndPage() throws TelegramApiException {
        when(classroom.getHomework()).thenReturn(items("Старое", 25), items("Новое", 1));
        when(sender.sendMessage(eq(CHAT), anyString(), any())).thenReturn(101)
                .thenThrow(new TelegramApiException("send failed"));
        handler.handle(USER, CHAT, 100);
        press(USER, CHAT, 101, "NEXT_PAGE");

        handler.handle(USER, CHAT, 200);
        press(USER, CHAT, 101, "NEXT_PAGE");

        verify(sender).editMessage(eq(CHAT), eq(101), contains("Старое 21\n"), any());
        verify(sender).sendFailureNotice(eq(CHAT), eq("Не удалось показать задания. Попробуй позже отправить /homework."), any());
        verify(sender, never()).deleteMessage(anyLong(), anyInt());
    }

    @Test
    void failedEditCanRetryTheSamePage() throws TelegramApiException {
        when(classroom.getHomework()).thenReturn(items("ДЗ", 25));
        when(sender.sendMessage(eq(CHAT), anyString(), any())).thenReturn(101);
        doThrow(new TelegramApiException("edit failed")).doNothing()
                .when(sender).editMessage(eq(CHAT), eq(101), anyString(), any());
        handler.handle(USER, CHAT, 100);

        press(USER, CHAT, 101, "NEXT_PAGE");
        press(USER, CHAT, 101, "NEXT_PAGE");
        verify(sender, times(2)).editMessage(eq(CHAT), eq(101), contains("ДЗ 11\n"), any());
        verify(sender).sendFailureNotice(eq(CHAT), eq("Не удалось перелистнуть страницу. Попробуй снова или отправь /homework."), any());
    }

    @Test
    void emptyReplyIsTrackedAndHasNoKeyboard() throws TelegramApiException {
        when(classroom.getHomework()).thenReturn(items("ДЗ", 2), List.of(), List.of());
        when(sender.sendMessage(eq(CHAT), anyString(), any())).thenReturn(101, 201, 301);
        handler.handle(USER, CHAT, 100);
        handler.handle(USER, CHAT, 200);
        handler.handle(USER, CHAT, 300);

        verify(sender, times(2)).sendMessage(CHAT, "Заданий пока нет.", null);
        for (int oldId : List.of(100, 101, 200, 201)) {
            verify(sender).deleteMessage(CHAT, oldId);
        }
        verify(sender, never()).sendFailureNotice(anyLong(), anyString(), any());
        verify(sender, never()).sendText(anyLong(), anyString());
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 101})
    void cleanupContinuesAfterEitherDeletionFails(int failedId) throws TelegramApiException {
        when(classroom.getHomework()).thenReturn(items("ДЗ", 2));
        when(sender.sendMessage(eq(CHAT), anyString(), any())).thenReturn(101, 201, 301);
        doAnswer(call -> {
            if (call.<Integer>getArgument(1) == failedId) throw new TelegramApiException("delete failed");
            return null;
        }).when(sender).deleteMessage(eq(CHAT), anyInt());

        handler.handle(USER, CHAT, 100);
        handler.handle(USER, CHAT, 200);
        handler.handle(USER, CHAT, 300);

        for (int oldId : List.of(100, 101, 200, 201)) {
            verify(sender).deleteMessage(CHAT, oldId);
        }
    }

    @Test
    void isolatesUsersAndChatsAndIgnoresRedelivery() throws TelegramApiException {
        when(classroom.getHomework()).thenReturn(items("Первое", 12), items("Другое", 2), items("Чужое", 12));
        when(sender.sendMessage(anyLong(), anyString(), any())).thenReturn(101, 101, 301);
        handler.handle(USER, CHAT, 100);
        handler.handle(USER, 2L, 100);
        handler.handle(20L, CHAT, 300);
        handler.handle(USER, CHAT, 100);

        press(20L, CHAT, 101, "NEXT_PAGE");
        verify(sender, never()).editMessage(anyLong(), anyInt(), anyString(), any());
        press(USER, CHAT, 101, "NEXT_PAGE");
        verify(sender).editMessage(eq(CHAT), eq(101), contains("Первое 11\n"), any());
        verify(sender, times(3)).sendMessage(anyLong(), anyString(), any());
        verify(sender, never()).deleteMessage(anyLong(), anyInt());
    }

    @Test
    void reportsClassroomFailure() {
        when(classroom.getHomework()).thenThrow(new ClassroomIntegrationException("failed", new IOException()));
        handler.handle(USER, CHAT, 100);
        verify(sender).sendText(CHAT, "Не удалось получить задания. Попробуй позже.");
        assertEquals("/homework", handler.command());
    }

    @Test
    void olderCommandCannotReplaceANewerReply() throws TelegramApiException {
        when(classroom.getHomework()).thenReturn(items("Новое", 12), items("Старое", 1));
        when(sender.sendMessage(eq(CHAT), anyString(), any())).thenReturn(201);

        handler.handle(USER, CHAT, 200);
        handler.handle(USER, CHAT, 100);
        press(USER, CHAT, 201, "NEXT_PAGE");

        verify(sender, times(1)).sendMessage(anyLong(), anyString(), any());
        verify(sender).editMessage(eq(CHAT), eq(201), contains("Новое 11\n"), any());
        verify(sender, never()).deleteMessage(anyLong(), anyInt());
    }

    private void press(long user, long chat, int messageId, String data) {
        buttons.handle("press", user, chat, messageId, data);
    }

    private List<HomeworkItem> items(String prefix, int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new HomeworkItem("Курс", prefix + " " + i, "url", LocalDateTime.of(2026, 10, 26, 14, 30)))
                .toList();
    }
}
