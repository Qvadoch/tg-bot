package com.example.tg_bot.classroom.converter;

import com.google.api.services.classroom.model.Date;
import com.google.api.services.classroom.model.TimeOfDay;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Преобразует дату и время срока из Google SDK в локальное время зоны бота.
 */
@Component
public class DueDateConverter {
    private final ZoneId zoneId;

    public DueDateConverter(@Value("${bot.timezone}") ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    /**
     * Преобразует срок с точностью до минуты.
     * При наличии времени интерпретирует его как UTC и переводит в зону бота.
     * При отсутствии времени использует 23:59 указанной даты в локальном представлении.
     * Секунды и наносекунды входящего времени не учитываются.
     *
     * @param date дата Google; может отсутствовать или быть неполной
     * @param dueTime время Google; отсутствующие часы и минуты считаются нулевыми
     * @return локальный срок либо {@code null}, если нет даты, года, месяца или дня
     */
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
