package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.homework.formatter.HomeworkFormatter;
import com.example.tg_bot.homework.model.HomeworkItem;
import com.example.tg_bot.homework.pagination.HomeworkPage;
import com.example.tg_bot.telegram.TelegramSender;
import com.example.tg_bot.telegram.TelegramTextSplitter;
import com.example.tg_bot.telegram.homework.HomeworkSessionStore.Conversation;
import com.example.tg_bot.telegram.homework.HomeworkSessionStore.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HomeworkViewLifecycleTest {
    private final TelegramSender sender = mock(TelegramSender.class);
    private final Clock clock = mock(Clock.class);
    private final Instant now = Instant.parse("2026-11-11T10:00:00Z");
    private final Conversation conversation = new Conversation(1, 10);
    private HomeworkSessionStore sessions;
    private HomeworkViewService views;

    @BeforeEach
    void setUp() throws TelegramApiException {
        when(clock.instant()).thenReturn(now);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        sessions = new HomeworkSessionStore(clock, 1, Duration.ofMinutes(30));
        views = new HomeworkViewService(sessions, sender,
                new HomeworkPageRenderer(new HomeworkFormatter(), new HomeworkKeyboard(), clock, new TelegramTextSplitter()));
        when(sender.sendMessage(anyLong(), anyString(), any())).thenReturn(101);
    }

    @Test
    void failedInitialSendDoesNotCreateASession() throws TelegramApiException {
        when(sender.sendMessage(anyLong(), anyString(), any())).thenThrow(new TelegramApiException("Send failed"));
        assertThrows(TelegramApiException.class, () -> views.show(1, 10, 100, items(1)));
        assertNull(sessions.find(conversation));
        verify(sender, never()).deleteMessage(anyLong(), anyInt());
    }

    @Test
    void expiredViewAsksForReloadWithoutEditing() throws TelegramApiException {
        views.show(1, 10, 100, items(12));
        when(clock.instant()).thenReturn(now.plusSeconds(1800));
        views.turnPage(1, 10, 101, PageAction.NEXT_PAGE);
        verify(sender).sendText(10L, "Чтобы загрузить задания, напиши /homework");
        verify(sender, never()).editMessage(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void evictedViewAsksForReloadWithoutEditing() throws TelegramApiException {
        views.show(1, 10, 100, items(12));
        views.show(2, 10, 200, items(1));
        views.turnPage(1, 10, 101, PageAction.NEXT_PAGE);
        verify(sender).sendText(10L, "Чтобы загрузить задания, напиши /homework");
        verify(sender, never()).editMessage(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void emptyViewDoesNotPerformEditsInEitherDirection() throws TelegramApiException {
        views.show(1, 10, 100, List.of());
        Session original = sessions.find(conversation);
        views.turnPage(1, 10, 101, PageAction.NEXT_PAGE);
        views.turnPage(1, 10, 101, PageAction.PREVIOUS_PAGE);
        assertSame(original, sessions.find(conversation));
        verify(sender, never()).editMessage(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void advancesTheSnapshotEvenWhenTwoPagesHaveIdenticalText() throws TelegramApiException {
        List<HomeworkItem> identical = IntStream.range(0, 21)
                .mapToObj(i -> new HomeworkItem("Java", "Одинаковое задание", "url", null)).toList();
        views.show(1, 10, 100, identical);
        HomeworkPage first = sessions.find(conversation).view().renderedPage().page();
        views.turnPage(1, 10, 101, PageAction.NEXT_PAGE);
        assertNotSame(first, sessions.find(conversation).view().renderedPage().page());
        verify(sender, never()).editMessage(anyLong(), anyInt(), anyString(), any());
        views.turnPage(1, 10, 101, PageAction.NEXT_PAGE);
        assertEquals(1, sessions.find(conversation).view().renderedPage().page().items().size());
        verify(sender).editMessage(eq(10L), eq(101), anyString(), any());
    }

    @Test
    @DisplayName("Текущее ограничение: повторный callback с тем же ID выполняет ещё один переход")
    void repeatedCallbackIsCurrentlyAppliedAgain() throws TelegramApiException {
        // Characterization of a known limitation, not a promise of idempotent callback handling.
        views.show(1, 10, 100, items(25));
        HomeworkCallbackHandler buttons = new HomeworkCallbackHandler(sender, views);
        buttons.handle("same-callback-id", 1L, 10L, 101, "NEXT_PAGE");
        buttons.handle("same-callback-id", 1L, 10L, 101, "NEXT_PAGE");
        assertTrue(sessions.find(conversation).view().text().contains("Работа 21\n"));
        verify(sender, times(2)).editMessage(eq(10L), eq(101), anyString(), any());
    }

    @Test
    void unknownButtonIsAcknowledgedWithoutChangingTheView() throws TelegramApiException {
        views.show(1, 10, 100, items(25));
        Session original = sessions.find(conversation);
        new HomeworkCallbackHandler(sender, views).handle("press", 1L, 10L, 101, "UNKNOWN_ACTION");
        assertSame(original, sessions.find(conversation));
        verify(sender).answerCallbackQuery("press");
        verify(sender, never()).editMessage(anyLong(), anyInt(), anyString(), any());
    }

    private List<HomeworkItem> items(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new HomeworkItem("Java", "Работа " + i, "url", null)).toList();
    }
}
