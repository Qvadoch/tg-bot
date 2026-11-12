package com.example.tg_bot;

import com.example.tg_bot.telegram.TelegramBot;
import com.google.api.services.classroom.Classroom;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "telegrambots.enabled=false", "bot.token=test-token", "bot.list.permitted.users-id=1",
        "google.client-file=unused", "google.tokens.dir=unused"
})
class TgBotApplicationTest {
    @MockitoBean Classroom classroom;
    @MockitoBean TelegramClient telegram;
    @Autowired TelegramBot bot;

    @Test
    void routesHelpThroughTheSpringApplication() throws TelegramApiException {
        Message message = mock(Message.class);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/help");
        when(message.getChatId()).thenReturn(1L);
        when(message.getFrom()).thenReturn(new User(1L, "Иван", false));
        Update update = new Update();
        update.setMessage(message);

        bot.consume(List.of(update));

        ArgumentCaptor<SendMessage> request = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegram).execute(request.capture());
        assertEquals("1", request.getValue().getChatId());
        assertTrue(request.getValue().getText().contains("/homework"));
    }
}
