package com.example.tg_bot.handler;

import com.example.tg_bot.bot.TelegramSender;
import com.example.tg_bot.google.classroom.ClassroomService;
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
    private static final Long CHAT_ID = 1L;

    @Mock
    TelegramSender telegramSender;
    @Mock
    ClassroomService classroomService;
    @InjectMocks
    CoursesHandler coursesHandler;

    @Test
    @DisplayName("есть курсы -> отправляет список курсов")
    void sendsCoursesList() throws IOException {
        when(classroomService.getCourses()).thenReturn(List.of("Java", "SQL"));

        coursesHandler.handle(CHAT_ID);

        verify(telegramSender).sendText(CHAT_ID, "Твои курсы:\nJava\nSQL");
    }

    @Test
    @DisplayName("курсов нет -> отправляет 'На данный момент курсов нет.'")
    void sendsMessageWhenNoCourses() throws IOException {
        when(classroomService.getCourses()).thenReturn(List.of());

        coursesHandler.handle(CHAT_ID);

        verify(telegramSender).sendText(CHAT_ID, "На данный момент курсов нет.");
    }

    @Test
    @DisplayName("Classroom бросает IOException -> отправляет сообщение об ошибке")
    void sendsErrorWhenClassroomFails() throws IOException {
        when(classroomService.getCourses()).thenThrow(new IOException("boom"));

        coursesHandler.handle(CHAT_ID);

        verify(telegramSender).sendText(CHAT_ID, "Не удалось получить курсы. Попробуй позже.");
    }

    @Test
    @DisplayName("вызывает метод command -> возвращает /courses")
    void commandIsCourses(){
        assertEquals("/courses", coursesHandler.command());
    }
}
