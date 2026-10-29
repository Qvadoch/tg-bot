package com.example.tg_bot.handler;

import com.example.tg_bot.format.HomeworkFormatter;
import com.example.tg_bot.model.HomeworkItem;
import com.example.tg_bot.bot.TelegramSender;
import com.example.tg_bot.google.classroom.ClassroomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class HomeworkHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(HomeworkHandler.class);

    private final TelegramSender telegramSender;
    private final ClassroomService classroomService;
    private final HomeworkFormatter homeworkFormatter;
    private final ZoneId zoneId;

    public HomeworkHandler(TelegramSender telegramSender, ClassroomService classroomService, HomeworkFormatter homeworkFormatter,
                           @Value("${bot.timezone}") ZoneId zoneId) {
        this.telegramSender = telegramSender;
        this.classroomService = classroomService;
        this.homeworkFormatter = homeworkFormatter;
        this.zoneId = zoneId;
    }

    @Override
    public String command() {
        return "/homework";
    }

    @Override
    public void handle(Long chatId) {
        try {
            List<HomeworkItem> homework = classroomService.getHomework();

            if (homework.isEmpty()) {
                telegramSender.sendText(chatId, "Заданий пока нет.");
                return;
            }

            LocalDateTime now = LocalDateTime.now(zoneId);
            String text = homeworkFormatter.format(homework, now);
            telegramSender.sendText(chatId, text);
        } catch (IOException e) {
            log.error("Не удалось получить задания из Classroom", e);
            telegramSender.sendText(chatId, "Не удалось получить задания. Попробуй позже.");
        }
    }
}
