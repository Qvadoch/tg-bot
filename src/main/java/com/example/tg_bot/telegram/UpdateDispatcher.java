package com.example.tg_bot.telegram;

import com.example.tg_bot.access.UserAccessChecker;
import com.example.tg_bot.telegram.homework.HomeworkCallbackHandler;
import com.example.tg_bot.telegram.command.CommandRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

/**
 * Проверяет отправителя и доступ, затем направляет текст команды или callback в обработчик.
 * Объекты Telegram разбираются здесь, а сценарии выполняются специализированными классами.
 */
@Component
public class UpdateDispatcher {

    private static final Logger log = LoggerFactory.getLogger(UpdateDispatcher.class);

    private final CommandRouter commandRouter;
    private final HomeworkCallbackHandler callbackHandler;
    private final UserAccessChecker userAccessChecker;

    public UpdateDispatcher(CommandRouter commandRouter, HomeworkCallbackHandler callbackHandler, UserAccessChecker userAccessChecker) {
        this.commandRouter = commandRouter;
        this.callbackHandler = callbackHandler;
        this.userAccessChecker = userAccessChecker;
    }

    /**
     * Выбирает путь обработки текстового сообщения или нажатия кнопки.
     * Обновления без поддерживаемого содержимого и {@code null} пропускаются.
     * Проверка доступа выполняется до вызова обработчиков.
     *
     * @param update обновление Telegram; может быть {@code null}
     */
    public void dispatch(Update update) {
        if (update == null) {
            log.warn("Ignoring a null Telegram update");
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    /**
     * Проверяет отправителя и извлекает команду из первого фрагмента текста.
     * Суффикс после {@code @} отбрасывается без проверки имени бота.
     *
     * @param message текстовое сообщение Telegram
     */
    private void handleTextMessage(Message message) {
        Long chatId = message.getChatId();
        User from = message.getFrom();
        if (from == null) {
            log.debug("Ignoring Telegram message without a sender in chat {}", chatId);
            return;
        }

        Long userId = from.getId();
        if (!userAccessChecker.isAllowed(userId)) {
            log.warn("Denied Telegram message from user {} in chat {}", userId, chatId);
            return;
        }

        Integer commandMessageId = message.getMessageId();
        String command = message.getText().strip().split("[\\s@]", 2)[0];
        commandRouter.route(userId, chatId, commandMessageId, command);
    }

    /**
     * Передаёт callback после проверки отправителя, доступа, сообщения и данных кнопки.
     *
     * @param callbackQuery нажатие кнопки Telegram
     */
    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        User from = callbackQuery.getFrom();
        if (from == null) {
            log.warn("Ignoring Telegram callback query without a sender");
            return;
        }

        Long userId = from.getId();
        if (!userAccessChecker.isAllowed(userId)) {
            log.warn("Denied Telegram callback query from user {}", userId);
            return;
        }

        if (callbackQuery.getMessage() == null) {
            log.debug("Ignoring Telegram callback query without a chat message");
            return;
        }

        String callbackQueryId = callbackQuery.getId();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String callbackData = callbackQuery.getData();
        if (callbackData == null) {
            log.debug("Ignoring Telegram callback query without data for message {} in chat {}", messageId, chatId);
            return;
        }

        callbackHandler.handle(callbackQueryId, userId, chatId, messageId, callbackData);
    }
}
