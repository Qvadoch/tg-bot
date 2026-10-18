package com.example.tg_bot;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class DueLabel {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static String format(LocalDateTime dueDate, LocalDateTime dateNow) {
        if (dueDate == null) {
            return "Без срока";
        }

        if (dueDate.isAfter(dateNow)) {
            Duration duration = Duration.between(dateNow, dueDate);
            long days = ChronoUnit.DAYS.between(dateNow.toLocalDate(), dueDate.toLocalDate());
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();

            if(days == 0) {
                if (hours == 0 && minutes == 0) {
                    return "Срок: сегодня, меньше минуты";
                } else if (hours == 0) {
                    return "Срок: сегодня через " + minutes + " мин";
                } else {
                    return "Срок: сегодня через " + hours + " ч" + (minutes == 0 ? "" : " " + minutes + " мин");
                }
            } else if(days == 1){
                return "Срок: завтра в " + dueDate.format(TIME_FORMAT);
            } else {
                String month = dueDate.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"));
                return "Срок: " + dueDate.getDayOfMonth() + " " + month + " " + dueDate.format(TIME_FORMAT);
            }
        }

        Duration duration = Duration.between(dueDate, dateNow);
        if (duration.getSeconds() >= 60 && duration.getSeconds() < 3600) {
            long minute = duration.getSeconds() / 60;
            return "Просрочено на " + minute + " мин";
        } else if (duration.getSeconds() < 60) {
            return "Просрочено меньше минуты";
        } else if (duration.getSeconds() < 86400) {
            long hour = duration.getSeconds() / 3600;
            long minute = duration.getSeconds() % 3600 / 60;
            return "Просрочено на " + hour + " ч" + (minute == 0 ? "" : " " + minute + " мин");
        } else {
            long day = duration.getSeconds() / 86400;
            return "Просрочено на " + day + " дн.";
        }
    }
}
