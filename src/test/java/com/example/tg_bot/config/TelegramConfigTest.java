package com.example.tg_bot.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramConfigTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TelegramConfig.class)
            .withPropertyValues("bot.token=test-token");

    @Test
    void createsTheClockInTheConfiguredZone() {
        contextRunner.withPropertyValues("bot.timezone=Asia/Kathmandu").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Asia/Kathmandu"));
        });
    }

    @Test
    void rejectsAnInvalidTimezoneAtStartup() {
        contextRunner.withPropertyValues("bot.timezone=Invalid/Timezone").run(context -> {
            assertThat(context).hasFailed();
        });
    }
}
