package com.example.tg_bot.handler;

import com.example.tg_bot.bot.TelegramSender;
import com.example.tg_bot.google.classroom.ClassroomService;
import com.example.tg_bot.model.HomeworkItem;
import com.example.tg_bot.format.HomeworkFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeworkHandlerTest {
    private static final Long CHAT_ID = 1L;
    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");

    @Mock
    private TelegramSender telegramSender;
    @Mock
    private ClassroomService  classroomService;
    @Mock
    private HomeworkFormatter homeworkFormatter;
    @Captor
    private ArgumentCaptor<LocalDateTime> nowCaptor;

    private HomeworkHandler homeworkHandler;

    @BeforeEach
    void setUp() {
        homeworkHandler = new HomeworkHandler(telegramSender, classroomService, homeworkFormatter, ZONE);
    }

    @Test
    @DisplayName("есть задания -> форматирует и отправляет текст")
    void sendsFormattedHomework() throws IOException{
        List<HomeworkItem> items = List.of(new HomeworkItem("Курсовая работа", "Курсач", "ссылка",
                LocalDateTime.of(2026, 10, 26, 14, 30)));

        when(classroomService.getHomework()).thenReturn(items);
        when(homeworkFormatter.format(eq(items), any(LocalDateTime.class))).thenReturn("готовый текст");

        homeworkHandler.handle(CHAT_ID);

        verify(telegramSender).sendText(CHAT_ID, "готовый текст");

    }

    @Test
    @DisplayName("форматтеру передаётся текущее время в часовом поясе хендлера")
    void passesCurrentTimeToFormatter() throws IOException{
        List<HomeworkItem> items = List.of(new HomeworkItem("Java", "Лаба", "ссылка", null));

        when(classroomService.getHomework()).thenReturn(items);
        when(homeworkFormatter.format(eq(items), any(LocalDateTime.class))).thenReturn("текст");

        homeworkHandler.handle(CHAT_ID);

        verify(homeworkFormatter).format(eq(items), nowCaptor.capture());
        LocalDateTime expected = LocalDateTime.now(ZONE);
        assertTrue(Duration.between(nowCaptor.getValue(), expected).abs().getSeconds() < 5);
    }

    @Test
    @DisplayName("заданий нет -> отправляет 'Заданий пока нет.', форматтер не вызывается")
    void sendsMessageWhenNoHomework() throws IOException {
        when(classroomService.getHomework()).thenReturn(List.of());

        homeworkHandler.handle(CHAT_ID);

        verify(telegramSender).sendText(CHAT_ID, "Заданий пока нет.");
        verifyNoInteractions(homeworkFormatter);
    }

    @Test
    @DisplayName("Classroom бросает IOException -> отправляет сообщение об ошибке")
    void sendsErrorWhenClassroomFails() throws IOException{
        when(classroomService.getHomework()).thenThrow(new IOException("boom"));

        homeworkHandler.handle(CHAT_ID);

        verify(telegramSender).sendText(CHAT_ID, "Не удалось получить задания. Попробуй позже.");
        verifyNoInteractions(homeworkFormatter);
    }

    @Test
    @DisplayName("command() возвращает '/homework'")
    void commandIsHomework() {
        assertEquals("/homework", homeworkHandler.command());
    }
}