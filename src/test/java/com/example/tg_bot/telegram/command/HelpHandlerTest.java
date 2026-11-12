package com.example.tg_bot.telegram.command;

import com.example.tg_bot.telegram.TelegramSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HelpHandlerTest {

    private static final Long USER_ID = 10L;
    private static final Long CHAT_ID = 1L;
    private static final Integer COMMAND_MESSAGE_ID = 100;

    @Mock
    TelegramSender telegramSender;

    @InjectMocks
    HelpHandler helpHandler;

    @Test
    @DisplayName("handle -> отправляет список команд")
    void handleSendsCommandsList() {
        helpHandler.handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);

        verify(telegramSender).sendText(CHAT_ID,
                "Команды:\n/start - начать\n/help - список команд\n" +
                        "/homework - показать домашние задания\n/courses - показать курсы");
    }

    @Test
    @DisplayName("command -> возвращает /help")
    void commandIsHelp() {
        assertEquals("/help", helpHandler.command());
    }
}
