package com.example.tg_bot.handler;


import com.example.tg_bot.bot.TelegramSender;
import com.example.tg_bot.google.classroom.ClassroomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

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
    public void handle(Long chatId) {
        try {
            List<String> courses = classroomService.getCourses();
            if (courses.isEmpty()) {
                telegramSender.sendText(chatId, "На данный момент курсов нет.");
                return;
            }
            telegramSender.sendText(chatId, "Твои курсы:\n" + String.join("\n", courses));
        } catch (IOException e) {
            log.error("Не удалось получить курсы из Classroom", e);
            telegramSender.sendText(chatId, "Не удалось получить курсы. Попробуй позже.");
        }
    }
}
