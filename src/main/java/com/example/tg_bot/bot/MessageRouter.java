package com.example.tg_bot.bot;

import com.example.tg_bot.handler.CommandHandler;
import com.example.tg_bot.handler.DefaultHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MessageRouter {
    private final Map<String, CommandHandler> handlers;
    private final DefaultHandler defaultHandler;

    public MessageRouter(List<CommandHandler> allHandlers, DefaultHandler defaultHandler) {
        this.handlers = allHandlers.stream()
                .collect(Collectors.toMap(CommandHandler::command, h -> h));
        this.defaultHandler = defaultHandler;
    }

    public void route(Long chatId, String command) {
        CommandHandler handler = handlers.get(command);
        if (handler == null) {
            defaultHandler.handle(chatId);
            return;
        }
        handler.handle(chatId);
    }

}