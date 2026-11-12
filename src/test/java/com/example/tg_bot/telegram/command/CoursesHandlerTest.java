package com.example.tg_bot.telegram.command;

import com.example.tg_bot.telegram.TelegramSender;
import com.example.tg_bot.classroom.service.ClassroomService;
import com.example.tg_bot.classroom.model.CourseData;
import com.example.tg_bot.classroom.exception.ClassroomIntegrationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CoursesHandlerTest {
    private static final Long USER_ID = 10L;
    private static final Long CHAT_ID = 1L;
    private static final Integer COMMAND_MESSAGE_ID = 100;

    @Mock
    TelegramSender telegramSender;
    @Mock
    ClassroomService classroomService;
    @InjectMocks
    CoursesHandler coursesHandler;

    @Test
    @DisplayName("есть курсы -> отправляет список курсов")
    void sendsCoursesList() {
        when(classroomService.getActiveCourses()).thenReturn(List.of(
                new CourseData("1", "Java"),
                new CourseData("2", "SQL")));

        coursesHandler.handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);

        verify(telegramSender).sendText(CHAT_ID, "Твои курсы:\nJava\nSQL");
    }

    @Test
    @DisplayName("курсов нет -> отправляет 'На данный момент курсов нет.'")
    void sendsMessageWhenNoCourses() {
        when(classroomService.getActiveCourses()).thenReturn(List.of());

        coursesHandler.handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);

        verify(telegramSender).sendText(CHAT_ID, "На данный момент курсов нет.");
    }

    @Test
    @DisplayName("ClassroomIntegrationException -> отправляет сообщение об ошибке")
    void sendsErrorWhenClassroomFails() {
        when(classroomService.getActiveCourses()).thenThrow(
                new ClassroomIntegrationException("Не удалось загрузить курсы", new IOException("boom")));

        coursesHandler.handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);

        verify(telegramSender).sendText(CHAT_ID, "Не удалось получить курсы. Попробуй позже.");
    }

    @Test
    @DisplayName("вызывает метод command -> возвращает /courses")
    void commandIsCourses(){
        assertEquals("/courses", coursesHandler.command());
    }
}
