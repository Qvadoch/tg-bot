package com.example.tg_bot.format;

import com.example.tg_bot.model.HomeworkItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HomeworkFormatter {
    public String format(List<HomeworkItem> homeworkItems, LocalDateTime now) {
        return homeworkItems.stream()
                .limit(10)
                .map(work -> work.courseName() + "\n"
                        + work.title() + "\n"
                        + DueLabel.format(work.due(), now) + "\n"
                        + work.link())
                .collect(Collectors.joining("\n\n", "Задания (" + homeworkItems.size() + "):\n\n", ""));
    }
}
