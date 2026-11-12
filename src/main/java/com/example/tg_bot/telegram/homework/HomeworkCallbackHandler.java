package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.telegram.TelegramSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Подтверждает нажатия кнопок и передаёт распознанные действия сервису просмотра.
 * Ошибка редактирования обрабатывается уведомлением пользователя.
 */
@Component
public class HomeworkCallbackHandler {
    private static final Logger log = LoggerFactory.getLogger(HomeworkCallbackHandler.class);

    private final TelegramSender sender;
    private final HomeworkViewService views;

    public HomeworkCallbackHandler(TelegramSender sender, HomeworkViewService views) {
        this.sender = sender;
        this.views = views;
    }

    /**
     * Подтверждает callback и выполняет распознанное действие навигации.
     * Неизвестные данные подтверждаются без изменения просмотра.
     * Повторные поступления одного {@code callbackQueryId} отдельно не отсеиваются.
     *
     * @param callbackQueryId ID нажатия для подтверждения Telegram
     * @param userId ID пользователя, уже прошедшего проверку доступа
     * @param chatId чат сообщения с кнопками
     * @param messageId ID сообщения, к которому относится нажатие
     * @param callbackData строковое имя {@link PageAction}
     */
    public void handle(String callbackQueryId, Long userId, Long chatId, Integer messageId, String callbackData) {
        sender.answerCallbackQuery(callbackQueryId);
        for (PageAction action : PageAction.values()) {
            if (action.name().equals(callbackData)) {
                try {
                    views.turnPage(userId, chatId, messageId, action);
                } catch (TelegramApiException exception) {
                    log.error("Failed to edit homework message {} in Telegram chat {}", messageId, chatId, exception);
                    sender.sendFailureNotice(chatId, "Не удалось перелистнуть страницу. Попробуй снова или отправь /homework.", exception);
                }
                return;
            }
        }
    }
}
