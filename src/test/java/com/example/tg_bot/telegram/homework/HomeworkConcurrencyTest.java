package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.homework.formatter.HomeworkFormatter;
import com.example.tg_bot.homework.model.HomeworkItem;
import com.example.tg_bot.telegram.TelegramSender;
import com.example.tg_bot.telegram.TelegramTextSplitter;
import com.example.tg_bot.telegram.homework.HomeworkSessionStore.Conversation;
import com.example.tg_bot.telegram.homework.HomeworkSessionStore.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(20)
class HomeworkConcurrencyTest {
    private final ExecutorService workers = Executors.newFixedThreadPool(2);
    private final TelegramSender sender = mock(TelegramSender.class);
    private final Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
    private final HomeworkSessionStore sessions = new HomeworkSessionStore(clock, 1000, Duration.ofMinutes(30));
    private final HomeworkViewService views = new HomeworkViewService(sessions, sender,
            new HomeworkPageRenderer(new HomeworkFormatter(), new HomeworkKeyboard(), clock, new TelegramTextSplitter()));

    @AfterEach
    void stopWorkers() throws InterruptedException {
        workers.shutdownNow();
        assertTrue(workers.awaitTermination(5, SECONDS), "Test workers must terminate");
    }

    @Test
    void concurrentTurnsUseTheLastSuccessfullySavedView() throws Exception {
        when(sender.sendMessage(anyLong(), anyString(), any())).thenReturn(101);
        views.show(1, 10, 100, items(25));
        Session original = sessions.find(new Conversation(1, 10));
        CountDownLatch firstEditEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstEdit = new CountDownLatch(1);
        AtomicInteger edits = new AtomicInteger();
        doAnswer(call -> {
            if (edits.incrementAndGet() == 1) {
                firstEditEntered.countDown();
                await(releaseFirstEdit);
            }
            return null;
        }).when(sender).editMessage(eq(10L), eq(101), anyString(), any());

        Future<?> first = workers.submit(() -> { views.turnPage(1, 10, 101, PageAction.NEXT_PAGE); return null; });
        Future<?> second;
        try {
            await(firstEditEntered);
            AtomicReference<Thread> secondThread = new AtomicReference<>();
            second = workers.submit(() -> {
                secondThread.set(Thread.currentThread());
                views.turnPage(1, 10, 101, PageAction.NEXT_PAGE);
                return null;
            });
            awaitBlocked(secondThread, second);
            assertSame(original, sessions.find(new Conversation(1, 10)));
            assertEquals(1, edits.get());
        } finally {
            releaseFirstEdit.countDown();
        }
        first.get(5, SECONDS);
        second.get(5, SECONDS);

        InOrder order = inOrder(sender);
        order.verify(sender).editMessage(eq(10L), eq(101), contains("Работа 11\n"), any());
        order.verify(sender).editMessage(eq(10L), eq(101), contains("Работа 21\n"), any());
        assertTrue(sessions.find(new Conversation(1, 10)).view().text().contains("Работа 21\n"));
    }

    @ParameterizedTest
    @ValueSource(ints = {99, 100})
    void queuedOlderOrRepeatedCommandCannotReplaceTheNewerReply(int queuedCommand) throws Exception {
        CountDownLatch sendEntered = new CountDownLatch(1);
        CountDownLatch releaseSend = new CountDownLatch(1);
        when(sender.sendMessage(anyLong(), anyString(), any())).thenAnswer(call -> {
            sendEntered.countDown();
            await(releaseSend);
            return 101;
        });
        Future<?> first = workers.submit(() -> { views.show(1, 10, 100, items(25)); return null; });
        Future<?> second;
        try {
            await(sendEntered);
            AtomicReference<Thread> secondThread = new AtomicReference<>();
            second = workers.submit(() -> {
                secondThread.set(Thread.currentThread());
                views.show(1, 10, queuedCommand, items(1));
                return null;
            });
            awaitBlocked(secondThread, second);
            assertNull(sessions.find(new Conversation(1, 10)));
        } finally {
            releaseSend.countDown();
        }
        first.get(5, SECONDS);
        second.get(5, SECONDS);

        verify(sender, times(1)).sendMessage(anyLong(), anyString(), any());
        verify(sender, never()).deleteMessage(anyLong(), anyInt());
        assertEquals(25, sessions.find(new Conversation(1, 10)).view().renderedPage().page().totalCount());
    }

    @Test
    void conversationOnAnotherLockCanFinishWhileTheFirstSendIsWaiting() throws Exception {
        Conversation firstConversation = new Conversation(1, 10);
        Conversation secondConversation = new Conversation(1, 11);
        // Different conversations may share a stripe; this case intentionally selects different stripes.
        assertNotSame(sessions.lockFor(firstConversation), sessions.lockFor(secondConversation));
        CountDownLatch firstSendEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstSend = new CountDownLatch(1);
        when(sender.sendMessage(anyLong(), anyString(), any())).thenAnswer(call -> {
            if (call.<Long>getArgument(0) == 10L) {
                firstSendEntered.countDown();
                await(releaseFirstSend);
                return 101;
            }
            return 201;
        });
        Future<?> first = workers.submit(() -> { views.show(1, 10, 100, items(1)); return null; });
        try {
            await(firstSendEntered);
            Future<?> second = workers.submit(() -> { views.show(1, 11, 200, items(2)); return null; });
            second.get(5, SECONDS);
            assertEquals(201, sessions.find(secondConversation).messages().botMessageId());
            assertNull(sessions.find(firstConversation));
        } finally {
            releaseFirstSend.countDown();
        }
        first.get(5, SECONDS);
        assertEquals(101, sessions.find(firstConversation).messages().botMessageId());
    }

    @Test
    void oldButtonWaitsForTheReplacementAndThenIsIgnored() throws Exception {
        when(sender.sendMessage(anyLong(), anyString(), any())).thenReturn(101);
        views.show(1, 10, 100, items(25));
        Session previous = sessions.find(new Conversation(1, 10));
        CountDownLatch replacementEntered = new CountDownLatch(1);
        CountDownLatch releaseReplacement = new CountDownLatch(1);
        when(sender.sendMessage(anyLong(), anyString(), any())).thenAnswer(call -> {
            replacementEntered.countDown();
            await(releaseReplacement);
            return 201;
        });

        Future<?> replacement = workers.submit(() -> { views.show(1, 10, 200, items(12)); return null; });
        Future<?> oldButton;
        try {
            await(replacementEntered);
            AtomicReference<Thread> buttonThread = new AtomicReference<>();
            oldButton = workers.submit(() -> {
                buttonThread.set(Thread.currentThread());
                views.turnPage(1, 10, 101, PageAction.NEXT_PAGE);
                return null;
            });
            awaitBlocked(buttonThread, oldButton);
            assertSame(previous, sessions.find(new Conversation(1, 10)));
        } finally {
            releaseReplacement.countDown();
        }
        replacement.get(5, SECONDS);
        oldButton.get(5, SECONDS);

        Session current = sessions.find(new Conversation(1, 10));
        assertEquals(201, current.messages().botMessageId());
        assertEquals(12, current.view().renderedPage().page().totalCount());
        assertTrue(current.view().text().contains("Работа 1\n"));
        verify(sender, never()).editMessage(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void queuedTurnUsesThePreviousViewAfterTheFirstEditFails() throws Exception {
        when(sender.sendMessage(anyLong(), anyString(), any())).thenReturn(101);
        views.show(1, 10, 100, items(25));
        Session previous = sessions.find(new Conversation(1, 10));
        CountDownLatch firstEditEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstEdit = new CountDownLatch(1);
        AtomicInteger edits = new AtomicInteger();
        TelegramApiException failure = new TelegramApiException("First edit failed");
        doAnswer(call -> {
            if (edits.incrementAndGet() == 1) {
                firstEditEntered.countDown();
                await(releaseFirstEdit);
                throw failure;
            }
            return null;
        }).when(sender).editMessage(eq(10L), eq(101), anyString(), any());

        Future<?> first = workers.submit(() -> { views.turnPage(1, 10, 101, PageAction.NEXT_PAGE); return null; });
        Future<?> second;
        try {
            await(firstEditEntered);
            AtomicReference<Thread> secondThread = new AtomicReference<>();
            second = workers.submit(() -> {
                secondThread.set(Thread.currentThread());
                views.turnPage(1, 10, 101, PageAction.NEXT_PAGE);
                return null;
            });
            awaitBlocked(secondThread, second);
            assertSame(previous, sessions.find(new Conversation(1, 10)));
        } finally {
            releaseFirstEdit.countDown();
        }
        ExecutionException error = assertThrows(ExecutionException.class, () -> first.get(5, SECONDS));
        assertSame(failure, error.getCause());
        second.get(5, SECONDS);

        verify(sender, times(2)).editMessage(eq(10L), eq(101), contains("Работа 11\n"), any());
        assertTrue(sessions.find(new Conversation(1, 10)).view().text().contains("Работа 11\n"));
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        assertTrue(latch.await(5, SECONDS), "Expected operation did not reach its checkpoint");
    }

    private static void awaitBlocked(AtomicReference<Thread> thread, Future<?> task) throws Exception {
        long deadline = System.nanoTime() + SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (task.isDone()) {
                task.get();
                fail("The second operation must wait for the unfinished operation in the same conversation");
            }
            Thread worker = thread.get();
            if (worker != null && worker.getState() == Thread.State.BLOCKED) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
        fail("The second operation did not reach the conversation lock");
    }

    private static List<HomeworkItem> items(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new HomeworkItem("Java", "Работа " + i, "url", null)).toList();
    }
}
