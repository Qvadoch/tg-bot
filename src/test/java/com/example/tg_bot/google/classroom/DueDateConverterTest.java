package com.example.tg_bot.google.classroom;

import com.google.api.services.classroom.model.Date;
import com.google.api.services.classroom.model.TimeOfDay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DueDateConverterTest {
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Moscow");

    private final DueDateConverter dueDateConverter = new DueDateConverter(ZONE_ID);

    @Test
    @DisplayName("даты нет -> null")
    void returnsNullWhenDateIsNull() {
        assertNull(dueDateConverter.getDue(null, null));
    }

    @Test
    @DisplayName("в дате нет года -> null")
    void returnsNullWhenYearIsMissing() {
        Date date = new Date().setMonth(10).setDay(25);

        assertNull(dueDateConverter.getDue(date, null));
    }

    @Test
    @DisplayName("в дате нет месяца -> null")
    void returnsNullWhenMonthIsMissing() {
        Date date = new Date().setYear(2026).setDay(25);

        assertNull(dueDateConverter.getDue(date, null));
    }

    @Test
    @DisplayName("в дате нет дня -> null")
    void returnsNullWhenDayIsMissing() {
        Date date = new Date().setYear(2026).setMonth(10);

        assertNull(dueDateConverter.getDue(date, null));
    }

    @Test
    @DisplayName("времени нет -> срок 23:59 того же дня")
    void returnsEndOfDayWhenTimeIsMissing() {
        Date date = new Date().setYear(2026).setMonth(10).setDay(25);

        assertEquals(LocalDateTime.of(2026, 10, 25, 23, 59), dueDateConverter.getDue(date, null));
    }

    @ParameterizedTest(name = "{0}:00 UTC -> {1}:00 в Москве, сдвиг дня: {2}")
    @CsvSource({
            "0,  3,  0",
            "9,  12, 0",
            "12, 15, 0",
            "21, 0,  1",
            "22, 1,  1"
    })
    @DisplayName("время в UTC переводится в зону бота, в том числе через полночь")
    void convertsUtcToBotZone(int utcHour, int expectedHour, int expectedDayShift) {
        Date date = new Date().setYear(2026).setMonth(10).setDay(25);
        TimeOfDay timeOfDay = new TimeOfDay().setHours(utcHour).setMinutes(0);
        LocalDateTime expected = LocalDateTime.of(2026, 10, 25, expectedHour, 0).plusDays(expectedDayShift);

        assertEquals(expected, dueDateConverter.getDue(date, timeOfDay));
    }

    @Test
    @DisplayName("31 декабря 22:00 UTC -> 1 января следующего года 01:00")
    void convertsAcrossNewYear() {
        Date date = new Date().setYear(2026).setMonth(12).setDay(31);
        TimeOfDay time = new TimeOfDay().setHours(22).setMinutes(0);

        assertEquals(LocalDateTime.of(2027, 1, 1, 1, 0), dueDateConverter.getDue(date, time));
    }

    @Test
    @DisplayName("в TimeOfDay не задан час -> час считается нулём")
    void treatsMissingHoursAsZero() {
        Date date = new Date().setYear(2026).setMonth(10).setDay(25);
        TimeOfDay time = new TimeOfDay().setMinutes(12);

        assertEquals(LocalDateTime.of(2026, 10, 25, 3, 12), dueDateConverter.getDue(date, time));
    }

    @Test
    @DisplayName("в TimeOfDay не заданы минуты -> минуты считаются нулём")
    void treatsMissingMinutesAsZero() {
        Date date = new Date().setYear(2026).setMonth(10).setDay(25);
        TimeOfDay time = new TimeOfDay().setHours(12);

        assertEquals(LocalDateTime.of(2026, 10, 25, 15, 0), dueDateConverter.getDue(date, time));
    }

    @Test
    @DisplayName("пустой TimeOfDay -> полночь UTC, то есть 03:00 по Москве")
    void treatsEmptyTimeAsMidnightUtc() {
        Date date = new Date().setYear(2026).setMonth(10).setDay(25);
        TimeOfDay time = new TimeOfDay();

        assertEquals(LocalDateTime.of(2026, 10, 25, 3, 0), dueDateConverter.getDue(date, time));
    }
}