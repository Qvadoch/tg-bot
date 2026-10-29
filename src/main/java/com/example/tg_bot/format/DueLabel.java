package com.example.tg_bot.format;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class DueLabel {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale RU = Locale.forLanguageTag("ru");

    private DueLabel() {
    }

    /**
     * Подпись срока для сообщения. {@code due} и {@code now} должны быть в одной зоне, {@code now} не null.
     */
    public static String format(LocalDateTime due, LocalDateTime now) {
        if (due == null) {
            return "Без срока";
        }
        if (due.isAfter(now)) {
            return futureLabel(due, now);
        }
        return overdueLabel(due, now);
    }

    private static String futureLabel(LocalDateTime due, LocalDateTime now) {
        long days = ChronoUnit.DAYS.between(now.toLocalDate(), due.toLocalDate());

        if (days == 0) {
            Duration left = Duration.between(now, due);
            if (left.toMinutes() < 1) {
                return "Срок: сегодня, меньше минуты";
            }
            return "Срок: сегодня через " + hoursAndMinutes(left.toHours(), left.toMinutesPart());
        }
        if (days == 1) {
            return "Срок: завтра в " + due.format(TIME_FORMAT);
        }
        String month = due.getMonth().getDisplayName(TextStyle.FULL, RU);
        return "Срок: " + due.getDayOfMonth() + " " + month + " " + due.format(TIME_FORMAT);
    }

    private static String overdueLabel(LocalDateTime due, LocalDateTime now) {
        Duration overdue = Duration.between(due, now);

        if (overdue.toMinutes() < 1) {
            return "Просрочено меньше минуты";
        }
        if (overdue.toDays() < 1) {
            return "Просрочено на " + hoursAndMinutes(overdue.toHours(), overdue.toMinutesPart());
        }
        return "Просрочено на " + overdue.toDays() + " дн.";
    }

    private static String hoursAndMinutes(long hours, long minutes) {
        if (hours == 0) {
            return minutes + " мин";
        }
        return hours + " ч" + (minutes == 0 ? "" : " " + minutes + " мин");
    }
}
