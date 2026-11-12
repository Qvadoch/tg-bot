package com.example.tg_bot;

import com.example.tg_bot.classroom.model.CourseData;
import com.example.tg_bot.classroom.model.CourseWorkData;
import com.example.tg_bot.classroom.exception.ClassroomIntegrationException;
import com.example.tg_bot.classroom.port.ClassroomClient;
import com.example.tg_bot.telegram.TelegramBot;
import com.google.api.services.classroom.Classroom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "telegrambots.enabled=false", "bot.token=test-token", "bot.list.permitted.users-id=1",
        "google.client-file=unused", "google.tokens.dir=unused"
})
class HomeworkApplicationTest {
    @MockitoBean Classroom google;
    @MockitoBean ClassroomClient classroom;
    @MockitoBean TelegramClient telegram;
    @Autowired TelegramBot bot;

    @Test
    void routesHomeworkAndTheNextButtonThroughTheSpringApplication() throws TelegramApiException {
        when(classroom.getActiveCourses()).thenReturn(List.of(new CourseData("java", "Java")));
        when(classroom.getHomework("java")).thenReturn(IntStream.rangeClosed(1, 12)
                .mapToObj(i -> new CourseWorkData("Работа " + i, "url", null)).toList());
        Message reply = new Message();
        reply.setMessageId(101);
        when(telegram.execute(any(SendMessage.class))).thenReturn(reply);
        when(telegram.execute(any(EditMessageText.class))).thenReturn(Boolean.TRUE);

        User user = new User(1L, "User", false);
        Message command = mock(Message.class);
        when(command.hasText()).thenReturn(true);
        when(command.getText()).thenReturn("/homework");
        when(command.getChatId()).thenReturn(10L);
        when(command.getMessageId()).thenReturn(100);
        when(command.getFrom()).thenReturn(user);
        Update commandUpdate = new Update();
        commandUpdate.setMessage(command);
        bot.consume(List.of(commandUpdate));

        Message shown = mock(Message.class);
        when(shown.getChatId()).thenReturn(10L);
        when(shown.getMessageId()).thenReturn(101);
        CallbackQuery button = new CallbackQuery();
        button.setId("next-1");
        button.setFrom(user);
        button.setMessage(shown);
        button.setData("NEXT_PAGE");
        Update buttonUpdate = new Update();
        buttonUpdate.setCallbackQuery(button);
        bot.consume(List.of(buttonUpdate));

        ArgumentCaptor<SendMessage> sent = ArgumentCaptor.forClass(SendMessage.class);
        ArgumentCaptor<EditMessageText> edited = ArgumentCaptor.forClass(EditMessageText.class);
        verify(telegram).execute(sent.capture());
        verify(telegram).execute(edited.capture());
        assertTrue(sent.getValue().getText().contains("Работа 10\n"));
        assertFalse(sent.getValue().getText().contains("Работа 11\n"));
        assertTrue(edited.getValue().getText().contains("Работа 11\n"));
        assertTrue(edited.getValue().getText().contains("Работа 12\n"));
        assertEquals(101, edited.getValue().getMessageId());
        assertEquals("10", edited.getValue().getChatId());
        verify(classroom, times(1)).getActiveCourses();
        verify(classroom, times(1)).getHomework("java");
    }

    @Test
    void sendsTheCompleteLongCourseListThroughTheSpringApplication() throws TelegramApiException {
        long chatId = -100600L;
        List<CourseData> courses = IntStream.rangeClosed(1, 300)
                .mapToObj(index -> new CourseData("course-" + index, "Курс " + index + " " + "я".repeat(40)))
                .toList();
        when(classroom.getActiveCourses()).thenReturn(courses);

        bot.consume(List.of(command(1, chatId, 100, "/courses@StudyBot")));

        ArgumentCaptor<SendMessage> requests = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegram, atLeast(2)).execute(requests.capture());
        String expected = courses.stream().map(CourseData::name).collect(Collectors.joining("\n", "Твои курсы:\n", ""));
        assertEquals(expected, requests.getAllValues().stream().map(SendMessage::getText)
                .collect(Collectors.joining()));
        assertTrue(requests.getAllValues().stream().allMatch(request -> request.getText().length() <= 4096));
        assertTrue(requests.getAllValues().stream()
                .allMatch(request -> String.valueOf(chatId).equals(request.getChatId())));
        verify(classroom).getActiveCourses();
        verify(telegram, never()).execute(any(EditMessageText.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"command", "button"})
    void deniesUnauthorizedEventsBeforeAnyExternalRequest(String event) {
        Update update = event.equals("command")
                ? command(2, -100601L, 100, "/homework")
                : button(2, -100601L, 101);

        bot.consume(List.of(update));

        verifyNoInteractions(classroom, telegram);
    }

    @Test
    void keepsTheExistingReplyUsableAfterAClassroomReloadFails() throws TelegramApiException {
        long chatId = -100700L;
        when(classroom.getActiveCourses()).thenReturn(List.of(new CourseData("java", "Java")));
        when(classroom.getHomework("java")).thenReturn(IntStream.rangeClosed(1, 12)
                .mapToObj(index -> new CourseWorkData("Старое " + index, "url", null)).toList())
                .thenThrow(new ClassroomIntegrationException("Reload failed", new IOException("Network failed")));
        Message reply = new Message();
        reply.setMessageId(101);
        when(telegram.execute(any(SendMessage.class))).thenReturn(reply);
        when(telegram.execute(any(EditMessageText.class))).thenReturn(Boolean.TRUE);

        bot.consume(List.of(command(1, chatId, 100, "/homework"),
                command(1, chatId, 200, "/homework"), button(1, chatId, 101)));

        ArgumentCaptor<SendMessage> sent = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegram, times(2)).execute(sent.capture());
        assertTrue(sent.getAllValues().getFirst().getText().contains("Старое 1\n"));
        assertEquals("Не удалось получить задания. Попробуй позже.", sent.getAllValues().getLast().getText());
        ArgumentCaptor<EditMessageText> edited = ArgumentCaptor.forClass(EditMessageText.class);
        verify(telegram).execute(edited.capture());
        assertTrue(edited.getValue().getText().contains("Старое 11\n"));
        assertEquals(101, edited.getValue().getMessageId());
        assertEquals(String.valueOf(chatId), edited.getValue().getChatId());
        verify(telegram, never()).execute(any(DeleteMessage.class));
        verify(classroom, times(2)).getHomework("java");
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 429})
    void doesNotSendAnExtraNoticeAfterTelegramDeniesTheMainReply(int code) throws TelegramApiException {
        when(classroom.getActiveCourses()).thenReturn(List.of());
        ApiResponse<Message> response = ApiResponse.<Message>builder()
                .errorCode(code).errorDescription("Request rejected").build();
        when(telegram.execute(any(SendMessage.class)))
                .thenThrow(new TelegramApiRequestException("Send rejected", response));

        assertDoesNotThrow(() -> bot.consume(List.of(command(1, -1008000L - code, 100, "/homework"))));

        verify(telegram).execute(any(SendMessage.class));
        verify(telegram, never()).execute(any(DeleteMessage.class));
        verify(telegram, never()).execute(any(EditMessageText.class));
    }

    private Update command(long userId, long chatId, int messageId, String text) {
        Message message = mock(Message.class);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(text);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getMessageId()).thenReturn(messageId);
        when(message.getFrom()).thenReturn(new User(userId, "User", false));
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private Update button(long userId, long chatId, int messageId) {
        Message shown = mock(Message.class);
        when(shown.getChatId()).thenReturn(chatId);
        when(shown.getMessageId()).thenReturn(messageId);
        CallbackQuery callback = new CallbackQuery();
        callback.setId("next-" + chatId);
        callback.setFrom(new User(userId, "User", false));
        callback.setMessage(shown);
        callback.setData("NEXT_PAGE");
        Update update = new Update();
        update.setCallbackQuery(callback);
        return update;
    }
}
