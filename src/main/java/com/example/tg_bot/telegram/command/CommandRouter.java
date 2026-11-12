package com.example.tg_bot.telegram.command;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Выбирает обработчик по точному имени команды; неизвестную команду передаёт обработчику по умолчанию.
 */
@Component
public class CommandRouter {
    private final Map<String, CommandHandler> handlers;
    private final DefaultHandler defaultHandler;

    /**
     * Создаёт реестр команд из обработчиков, предоставленных Spring.
     *
     * @param allHandlers обработчики с уникальными именами команд
     * @param defaultHandler обработчик неизвестной команды
     * @throws IllegalStateException если два обработчика объявляют одинаковое имя команды
     */
    public CommandRouter(List<CommandHandler> allHandlers, DefaultHandler defaultHandler) {
        this.handlers = allHandlers.stream()
                .collect(Collectors.toMap(CommandHandler::command, h -> h));
        this.defaultHandler = defaultHandler;
    }

    /**
     * Находит и вызывает обработчик уже извлечённого имени команды.
     *
     * @param userId ID пользователя
     * @param chatId ID чата
     * @param commandMessageId ID сообщения с командой
     * @param command имя команды после обработки диспетчером
     */
    public void route(Long userId, Long chatId, Integer commandMessageId, String command) {
        CommandHandler handler = handlers.get(command);
        if (handler == null) {
            defaultHandler.handle(chatId);
            return;
        }
        handler.handle(userId, chatId, commandMessageId);
    }
}
