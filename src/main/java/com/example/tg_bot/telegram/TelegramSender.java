package com.example.tg_bot.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Выполняет запросы Telegram API и определяет, какие ошибки передать вызывающему коду.
 * Основная отправка, редактирование и удаление передают ошибки; вспомогательные
 * уведомления и подтверждения callback записывают их в журнал.
 */
@Component
public class TelegramSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramSender.class);
    private final TelegramClient telegramClient;
    private final TelegramTextSplitter textSplitter;

    public TelegramSender(TelegramClient telegramClient, TelegramTextSplitter textSplitter) {
        this.telegramClient = telegramClient;
        this.textSplitter = textSplitter;
    }

    /**
     * Отправляет все части простого текста последовательно.
     * При первой ошибке прекращает отправку оставшихся частей и записывает ошибку в журнал.
     *
     * @param chatId ID чата
     * @param text текст для разбиения и отправки
     */
    public void sendText(Long chatId, String text) {
        for (String part : textSplitter.split(text)) {
            SendMessage sendMessage = SendMessage.builder().chatId(chatId).text(part).build();
            try {
                telegramClient.execute(sendMessage);
            } catch (TelegramApiException exception) {
                log.error("Failed to send text to Telegram chat {}", chatId, exception);
                return;
            }
        }
    }

    /**
     * Пытается сообщить о сбое, кроме ошибок доступа 401/403 и ограничения запросов 429.
     * Ошибки самого уведомления обрабатываются внутри {@link #sendText(Long, String)}.
     *
     * @param chatId ID чата
     * @param text текст уведомления
     * @param failure ошибка основной операции
     */
    public void sendFailureNotice(Long chatId, String text, TelegramApiException failure) {
        if (failure instanceof TelegramApiRequestException rejection) {
            Integer code = rejection.getErrorCode();
            // A second request cannot bypass an authorization error or a rate limit.
            if (code != null && (code == 401 || code == 403 || code == 429)) {
                return;
            }
        }
        sendText(chatId, text);
    }

    /**
     * Отправляет одно сообщение и возвращает полученный от Telegram ID.
     * Вызывающий код заранее подготавливает текст допустимой длины.
     *
     * @param chatId ID чата
     * @param text подготовленный текст
     * @param inlineKeyboardMarkup клавиатура либо {@code null}
     * @return ID подтверждённого ответа бота
     * @throws TelegramApiException если отправка не подтверждена; сообщение могло быть доставлено при потере сетевого ответа
     */
    public Integer sendMessage(Long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) throws TelegramApiException {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(inlineKeyboardMarkup)
                .build();

        Message message = telegramClient.execute(sendMessage);

        return message.getMessageId();
    }

    /**
     * Подтверждает обработку нажатия, чтобы Telegram мог завершить индикатор ожидания.
     * Ошибка подтверждения записывается в журнал и не прерывает дальнейшую обработку кнопки.
     *
     * @param callbackQueryId ID нажатия
     */
    public void answerCallbackQuery(String callbackQueryId) {
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(callbackQueryId);
        try {
            telegramClient.execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            log.warn("Failed to acknowledge Telegram callback query {}", callbackQueryId, e);
        }
    }

    /**
     * Редактирует текст и клавиатуру указанного сообщения.
     * Ответ 400 с описанием {@code message is not modified} считается подтверждением
     * уже применённого содержимого; остальные ошибки передаются вызывающему коду.
     *
     * @param chatId ID чата
     * @param messageId ID редактируемого ответа
     * @param text новый подготовленный текст
     * @param inlineKeyboardMarkup новая клавиатура либо {@code null}
     * @throws TelegramApiException если редактирование не подтверждено
     */
    public void editMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) throws TelegramApiException {
        EditMessageText editMessageText = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text)
                .replyMarkup(inlineKeyboardMarkup)
                .build();
        try {
            telegramClient.execute(editMessageText);
        } catch (TelegramApiRequestException exception) {
            String description = exception.getApiResponse();
            if (Integer.valueOf(400).equals(exception.getErrorCode()) && description != null
                    && description.contains("message is not modified")) {
                log.debug("Telegram message {} in chat {} already has the requested content", messageId, chatId);
                return;
            }
            throw exception;
        }
    }

    /**
     * Удаляет указанное сообщение, передавая решение об обработке ошибки вызывающему коду.
     *
     * @param chatId ID чата
     * @param messageId ID удаляемого сообщения
     * @throws TelegramApiException если удаление не подтверждено
     */
    public void deleteMessage(Long chatId, Integer messageId) throws TelegramApiException {
        DeleteMessage deleteMessage = DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();
        telegramClient.execute(deleteMessage);
    }
}
