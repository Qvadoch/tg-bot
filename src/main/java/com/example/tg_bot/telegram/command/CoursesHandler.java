package com.example.tg_bot.telegram.command;


import com.example.tg_bot.classroom.model.CourseData;
import com.example.tg_bot.classroom.exception.ClassroomIntegrationException;
import com.example.tg_bot.telegram.TelegramSender;
import com.example.tg_bot.classroom.service.ClassroomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Обрабатывает {@code /courses}: получает активные курсы и отправляет их названия.
 * Длинный ответ разбивается отправителем; ошибка Classroom превращается в уведомление.
 */
@Component
public class CoursesHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(CoursesHandler.class);

    private final ClassroomService classroomService;
    private final TelegramSender telegramSender;

    public CoursesHandler(ClassroomService classroomService, TelegramSender telegramSender) {
        this.classroomService = classroomService;
        this.telegramSender = telegramSender;
    }

    @Override
    public String command() {
        return "/courses";
    }

    @Override
    public void handle(Long userId, Long chatId, Integer commandMessageId) {
        try {
            List<CourseData> courses = classroomService.getActiveCourses();
            if (courses.isEmpty()) {
                telegramSender.sendText(chatId, "На данный момент курсов нет.");
                return;
            }
            List<String> courseNames = new ArrayList<>();
            for (CourseData course : courses) {
                courseNames.add(course.name());
            }
            telegramSender.sendText(chatId, "Твои курсы:\n" + String.join("\n", courseNames));
        } catch (ClassroomIntegrationException exception) {
            log.error("Failed to handle /courses for Telegram chat {}", chatId, exception);
            telegramSender.sendText(chatId, "Не удалось получить курсы. Попробуй позже.");
        }
    }
}
