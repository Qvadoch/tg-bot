package com.example.tg_bot.homework.formatter;

import com.example.tg_bot.homework.model.HomeworkItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Формирует текст заданий с названиями курсов, сроками и ссылками.
 * Выбор страницы и разбиение длинного текста выполняются вызывающим кодом.
 */
@Component
public class HomeworkFormatter {
    /**
     * Форматирует все переданные задания, используя общее количество в заголовке.
     *
     * @param homeworkItems задания выбранной страницы в порядке показа
     * @param now время расчёта относительных сроков, в той же зоне, что и сроки заданий
     * @param totalHomeworkCount количество заданий во всём снимке
     * @return текст страницы; при пустом списке содержит только заголовок
     */
    public String format(List<HomeworkItem> homeworkItems, LocalDateTime now, int totalHomeworkCount) {
        return homeworkItems.stream()
                .map(work -> work.courseName() + "\n"
                        + work.title() + "\n"
                        + DueLabel.format(work.due(), now) + "\n"
                        + work.link())
                .collect(Collectors.joining("\n\n", "Задания (" + totalHomeworkCount + "):\n\n", ""));
    }
}
