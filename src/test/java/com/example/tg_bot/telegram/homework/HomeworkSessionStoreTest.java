package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.homework.pagination.HomeworkPage;
import com.example.tg_bot.telegram.homework.HomeworkSessionStore.Conversation;
import com.example.tg_bot.telegram.homework.HomeworkSessionStore.Session;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HomeworkSessionStoreTest {
    private final Clock clock = mock(Clock.class);
    private final Instant initialTime = Instant.parse("2026-11-08T10:00:00Z");
    private final Conversation first = new Conversation(1, 10);
    private final Session session = new Session(new HomeworkMessagePair(10L, 101, 100),
            new HomeworkView(new RenderedHomeworkPage(HomeworkPage.first(List.of()), List.of("Заданий пока нет."), null), 0));

    @Test
    void readingRefreshesIdleTimeoutAndExpiredSessionsDisappear() {
        when(clock.instant()).thenReturn(initialTime);
        HomeworkSessionStore store = new HomeworkSessionStore(clock, 2, Duration.ofMinutes(30));
        store.save(first, session);

        when(clock.instant()).thenReturn(initialTime.plusSeconds(1200));
        assertSame(session, store.find(first));
        when(clock.instant()).thenReturn(initialTime.plusSeconds(2400));
        assertSame(session, store.find(first));
        when(clock.instant()).thenReturn(initialTime.plusSeconds(4200));
        assertNull(store.find(first));
    }

    @Test
    void evictsTheLeastRecentlyUsedConversationAtCapacity() {
        when(clock.instant()).thenReturn(initialTime);
        HomeworkSessionStore store = new HomeworkSessionStore(clock, 2, Duration.ofMinutes(30));
        Conversation second = new Conversation(2, 10);
        Conversation third = new Conversation(3, 10);
        store.save(first, session);
        store.save(second, session);
        assertSame(session, store.find(first));

        store.save(third, session);

        assertNull(store.find(second));
        assertSame(session, store.find(first));
        assertSame(session, store.find(third));
    }

    @Test
    void conversationLockSurvivesEvictionAndExpiry() {
        when(clock.instant()).thenReturn(initialTime);
        HomeworkSessionStore store = new HomeworkSessionStore(clock, 1, Duration.ofMinutes(30));
        Object lock = store.lockFor(first);
        store.save(first, session);
        store.save(new Conversation(2, 10), session);
        assertNull(store.find(first));
        assertSame(lock, store.lockFor(new Conversation(1, 10)));

        store.save(first, session);
        when(clock.instant()).thenReturn(initialTime.plusSeconds(1800));
        assertNull(store.find(first));
        assertSame(lock, store.lockFor(first));
    }
}
