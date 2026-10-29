package com.example.tg_bot.google.classroom;

import com.google.api.services.classroom.model.Date;
import com.google.api.services.classroom.model.TimeOfDay;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class DueDateConverter {
    private final ZoneId zoneId;

    public DueDateConverter(@Value("${bot.timezone}") ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    public LocalDateTime getDue(Date date, TimeOfDay dueTime) {
        if (date == null || date.getYear() == null || date.getMonth() == null || date.getDay() == null) {
            return null;
        }
        int year = date.getYear();
        int month = date.getMonth();
        int day = date.getDay();

        if (dueTime == null) {
            return LocalDateTime.of(year, month, day, 23, 59);
        }

        int hour = dueTime.getHours() != null ? dueTime.getHours() : 0;
        int minute = dueTime.getMinutes() != null ? dueTime.getMinutes() : 0;

        ZonedDateTime utc = LocalDateTime.of(year, month, day, hour, minute).atZone(ZoneOffset.UTC);
        return utc.withZoneSameInstant(zoneId).toLocalDateTime();
    }
}
