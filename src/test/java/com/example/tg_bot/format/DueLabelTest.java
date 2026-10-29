package com.example.tg_bot.format;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DueLabelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 25, 12, 0);

    @Test
    @DisplayName("due = null -> 'Без срока'")
    void nullDueReturnsNoDeadline() {
        assertEquals("Без срока", DueLabel.format(null, NOW));
    }

    @Test
    @DisplayName("due через 30 минут -> 'Срок: сегодня через 30 мин'")
    void futureDueIn30Minutes() {
        LocalDateTime due = NOW.plusMinutes(30);
        assertEquals("Срок: сегодня через 30 мин", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due через 1 час -> 'Срок: сегодня через 1 ч'")
    void futureDueIn1Hour() {
        LocalDateTime due = NOW.plusHours(1);
        assertEquals("Срок: сегодня через 1 ч", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due через 2 часа 15 минут -> 'Срок: сегодня через 2 ч 15 мин'")
    void futureDueIn2Hours15Minutes() {
        LocalDateTime due = NOW.plusHours(2).plusMinutes(15);
        assertEquals("Срок: сегодня через 2 ч 15 мин", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due через 23 часа (в тот же день) -> 'Срок: сегодня через 23 ч'")
    void futureDueIn23HoursSameDay() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 0, 30);
        LocalDateTime due = now.plusHours(23);
        assertEquals("Срок: сегодня через 23 ч", DueLabel.format(due, now));
    }

    @Test
    @DisplayName("due через 23 часа (переход на завтра) -> 'Срок: завтра в 11:00'")
    void futureDueIn23HoursCrossesToTomorrow() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 12, 0);
        LocalDateTime due = now.plusHours(23);
        assertEquals("Срок: завтра в 11:00", DueLabel.format(due, now));
    }

    @Test
    @DisplayName("due меньше минуты вперёд -> 'Срок: сегодня, меньше минуты'")
    void futureDueLessThanMinute() {
        LocalDateTime due = NOW.plusSeconds(30);
        assertEquals("Срок: сегодня, меньше минуты", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due завтра в 14:30 -> 'Срок: завтра в 14:30'")
    void futureDueTomorrow() {
        LocalDateTime due = LocalDateTime.of(2026, 10, 26, 14, 30);
        assertEquals("Срок: завтра в 14:30", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due завтра в 09:00 -> 'Срок: завтра в 09:00'")
    void futureDueTomorrowMorning() {
        LocalDateTime due = LocalDateTime.of(2026, 10, 26, 9, 0);
        assertEquals("Срок: завтра в 09:00", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due через 5 дней -> 'Срок: 30 октября 12:00'")
    void futureDueIn5Days() {
        LocalDateTime due = NOW.plusDays(5);
        assertEquals("Срок: 30 октября 12:00", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due через 7 дней -> 'Срок: 1 ноября 12:00'")
    void futureDueIn7Days() {
        LocalDateTime due = NOW.plusDays(7);
        assertEquals("Срок: 1 ноября 12:00", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due через 30 дней -> 'Срок: 24 ноября 12:00'")
    void futureDueIn30Days() {
        LocalDateTime due = NOW.plusDays(30);
        assertEquals("Срок: 24 ноября 12:00", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due == now -> 'Просрочено меньше минуты'")
    void dueExactlyNow() {
        assertEquals("Просрочено меньше минуты", DueLabel.format(NOW, NOW));
    }

    @Test
    @DisplayName("due 30 секунд назад -> 'Просрочено меньше минуты'")
    void overdueLessThanMinute() {
        LocalDateTime due = NOW.minusSeconds(30);
        assertEquals("Просрочено меньше минуты", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due 2 часа назад -> 'Просрочено на 2 ч'")
    void overdueBy2Hours() {
        LocalDateTime due = NOW.minusHours(2);
        assertEquals("Просрочено на 2 ч", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due 1 час назад -> 'Просрочено на 1 ч'")
    void overdueBy1Hour() {
        LocalDateTime due = NOW.minusHours(1);
        assertEquals("Просрочено на 1 ч", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due 1 час 15 минут назад -> 'Просрочено на 1 ч 15 мин'")
    void overdueBy1Hour15Minutes() {
        LocalDateTime due = NOW.minusHours(1).minusMinutes(15);
        assertEquals("Просрочено на 1 ч 15 мин", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due 30 минут назад -> 'Просрочено на 30 мин'")
    void overdueBy30Minutes() {
        LocalDateTime due = NOW.minusMinutes(30);
        assertEquals("Просрочено на 30 мин", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due 23 часа назад -> 'Просрочено на 23 ч'")
    void overdueBy23Hours() {
        LocalDateTime due = NOW.minusHours(23);
        assertEquals("Просрочено на 23 ч", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due 1 день назад -> 'Просрочено на 1 дн.'")
    void overdueBy1Day() {
        LocalDateTime due = NOW.minusDays(1);
        assertEquals("Просрочено на 1 дн.", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due 3 дня назад -> 'Просрочено на 3 дн.'")
    void overdueBy3Days() {
        LocalDateTime due = NOW.minusDays(3);
        assertEquals("Просрочено на 3 дн.", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due 30 дней назад -> 'Просрочено на 30 дн.'")
    void overdueBy30Days() {
        LocalDateTime due = NOW.minusDays(30);
        assertEquals("Просрочено на 30 дн.", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due завтра в то же время -> 'Срок: завтра в 12:00'")
    void futureDueTomorrowSameTime() {
        LocalDateTime due = LocalDateTime.of(2026, 10, 26, 12, 0);
        assertEquals("Срок: завтра в 12:00", DueLabel.format(due, NOW));
    }

    @Test
    @DisplayName("due 25 часов назад -> 'Просрочено на 1 дн.'")
    void overdueBy25Hours() {
        LocalDateTime due = NOW.minusHours(25);
        assertEquals("Просрочено на 1 дн.", DueLabel.format(due, NOW));
    }
}