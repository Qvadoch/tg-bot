package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.homework.pagination.HomeworkPage;
import com.example.tg_bot.telegram.homework.HomeworkSessionStore.Conversation;
import com.example.tg_bot.telegram.homework.HomeworkSessionStore.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HomeworkSessionStoreContractTest {
    private final Clock clock = mock(Clock.class);
    private final Instant now = Instant.parse("2026-11-11T10:00:00Z");
    private final Conversation conversation = new Conversation(1, 10);

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveCapacity(int capacity) {
        assertThrows(IllegalArgumentException.class,
                () -> new HomeworkSessionStore(clock, capacity, Duration.ofMinutes(30)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveIdleTimeout(int seconds) {
        assertThrows(IllegalArgumentException.class,
                () -> new HomeworkSessionStore(clock, 2, Duration.ofSeconds(seconds)));
    }

    @Test
    void replacingASessionRefreshesItsLifetimeWithoutEvictingAnotherConversation() {
        when(clock.instant()).thenReturn(now);
        HomeworkSessionStore store = new HomeworkSessionStore(clock, 2, Duration.ofMinutes(30));
        Conversation other = new Conversation(2, 10);
        Session otherSession = session("other");
        store.save(conversation, session("old"));
        store.save(other, otherSession);
        when(clock.instant()).thenReturn(now.plusSeconds(1700));
        Session replacement = session("new");
        store.save(conversation, replacement);
        assertSame(otherSession, store.find(other));
        when(clock.instant()).thenReturn(now.plusSeconds(1800));
        assertSame(replacement, store.find(conversation));
    }

    @Test
    void savingAnotherConversationKeepsOnlyLiveSessions() {
        when(clock.instant()).thenReturn(now);
        HomeworkSessionStore store = new HomeworkSessionStore(clock, 2, Duration.ofMinutes(30));
        store.save(conversation, session("expired"));
        when(clock.instant()).thenReturn(now.plusSeconds(1800));
        Conversation fresh = new Conversation(2, 10);
        Session current = session("fresh");
        store.save(fresh, current);
        assertNull(store.find(conversation));
        assertSame(current, store.find(fresh));
    }

    private Session session(String text) {
        return new Session(new HomeworkMessagePair(10L, 101, 100),
                new HomeworkView(new RenderedHomeworkPage(HomeworkPage.first(List.of()), List.of(text), null), 0));
    }
}
