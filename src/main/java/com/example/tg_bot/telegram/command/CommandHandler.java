package com.example.tg_bot.telegram.command;

/**
 * Контракт обработчика одной текстовой команды Telegram.
 */
public interface CommandHandler {
    /**
     * Определяет команду, по которой роутер выбирает этот обработчик.
     *
     * @return имя команды с начальным {@code /}, без аргументов и суффикса имени бота
     */
    String command();
    /**
     * Выполняет сценарий команды после проверки доступа диспетчером.
     *
     * @param userId Telegram ID отправителя
     * @param chatId чат, в котором получена команда
     * @param commandMessageId ID сообщения с командой
     */
    void handle(Long userId, Long chatId, Integer commandMessageId);
}
