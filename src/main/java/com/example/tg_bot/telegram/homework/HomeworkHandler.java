package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.classroom.exception.ClassroomIntegrationException;
import com.example.tg_bot.classroom.service.ClassroomService;
import com.example.tg_bot.telegram.TelegramSender;
import com.example.tg_bot.telegram.command.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Обрабатывает {@code /homework}: загружает задания и передаёт их сервису показа.
 * Ошибки Classroom и Telegram преобразует в сообщения пользователю.
 * Загрузка выполняется до проверки повторной команды внутри сервиса показа.
 */
@Component
public class HomeworkHandler implements CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(HomeworkHandler.class);

    private final ClassroomService classroomService;
    private final HomeworkViewService views;
    private final TelegramSender sender;

    public HomeworkHandler(ClassroomService classroomService, HomeworkViewService views, TelegramSender sender) {
        this.classroomService = classroomService;
        this.views = views;
        this.sender = sender;
    }

    @Override
    public String command() {
        return "/homework";
    }

    @Override
    public void handle(Long userId, Long chatId, Integer commandMessageId) {
        try {
            views.show(userId, chatId, commandMessageId, classroomService.getHomework());
        } catch (ClassroomIntegrationException exception) {
            log.error("Failed to load homework from Google Classroom for Telegram chat {}", chatId, exception);
            sender.sendText(chatId, "Не удалось получить задания. Попробуй позже.");
        } catch (TelegramApiException exception) {
            log.error("Failed to send homework to Telegram chat {} for command message {}", chatId, commandMessageId, exception);
            sender.sendFailureNotice(chatId, "Не удалось показать задания. Попробуй позже отправить /homework.", exception);
        }
    }
}
