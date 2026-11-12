package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.homework.pagination.HomeworkPage;
import com.example.tg_bot.telegram.homework.HomeworkSessionStore.Conversation;
import com.example.tg_bot.telegram.homework.HomeworkSessionStore.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.convert.ApplicationConversionService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HomeworkSessionStoreConfigurationTest {
    private final Clock clock = mock(Clock.class);
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withBean(Clock.class, () -> clock)
            .withUserConfiguration(HomeworkSessionStore.class);

    @Test
    void defaultsToOneThousandSessionsAndThirtyMinutesOfInactivity() {
        when(clock.instant()).thenReturn(Instant.EPOCH);
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            HomeworkSessionStore store = context.getBean(HomeworkSessionStore.class);
            Session session = session();
            for (int user = 0; user <= 1000; user++) {
                store.save(new Conversation(user, 10), session);
            }
            assertThat(store.find(new Conversation(0, 10))).isNull();
            assertThat(store.find(new Conversation(1, 10))).isSameAs(session);
            when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(1800));
            assertThat(store.find(new Conversation(1, 10))).isNull();
        });
    }

    @Test
    void appliesTheConfiguredCapacityAndDuration() {
        when(clock.instant()).thenReturn(Instant.EPOCH);
        contextRunner.withPropertyValues("bot.homework.sessions.max-size=1",
                "bot.homework.sessions.idle-timeout=PT2S").run(context -> {
            assertThat(context).hasNotFailed();
            HomeworkSessionStore store = context.getBean(HomeworkSessionStore.class);
            Conversation first = new Conversation(1, -100500);
            Conversation second = new Conversation(2, -100500);
            store.save(first, session());
            when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(1));
            store.save(second, session());
            assertThat(store.find(first)).isNull();
            assertThat(store.find(second)).isNotNull();
            when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(3));
            assertThat(store.find(second)).isNull();
        });
    }

    @ParameterizedTest
    @CsvSource({"0, PT1S", "1, PT0S", "1, PT-1S"})
    void rejectsNonPositiveSettingsDuringSpringStartup(int size, String timeout) {
        contextRunner.withPropertyValues("bot.homework.sessions.max-size=" + size,
                "bot.homework.sessions.idle-timeout=" + timeout).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
        });
    }

    private Session session() {
        RenderedHomeworkPage page = new RenderedHomeworkPage(HomeworkPage.first(List.of()), List.of("text"), null);
        return new Session(new HomeworkMessagePair(10L, 101, 100), new HomeworkView(page, 0));
    }
}
