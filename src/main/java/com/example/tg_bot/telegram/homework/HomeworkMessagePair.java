package com.example.tg_bot.telegram.homework;

/**
 * Идентификаторы сообщений одного успешного показа заданий.
 * Используются для проверки активного ответа и удаления прежней пары сообщений.
 *
 * @param chatId чат, в котором выполнен показ
 * @param botMessageId ID отправленного ответа бота
 * @param commandMessageId ID пользовательской команды, вызвавшей показ
 */
record HomeworkMessagePair(Long chatId, Integer botMessageId, Integer commandMessageId) {
}
