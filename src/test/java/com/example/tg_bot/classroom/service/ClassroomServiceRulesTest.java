package com.example.tg_bot.classroom.service;

import com.example.tg_bot.classroom.exception.ClassroomIntegrationException;
import com.example.tg_bot.classroom.model.CourseData;
import com.example.tg_bot.classroom.model.CourseWorkData;
import com.example.tg_bot.classroom.port.ClassroomClient;
import com.example.tg_bot.homework.model.HomeworkItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClassroomServiceRulesTest {
    private final ClassroomClient client = mock(ClassroomClient.class);
    private final ClassroomService service = new ClassroomService(client);

    @Test
    void combinesCoursesAndSortsDeadlinesDescendingWithMissingDatesLast() {
        LocalDateTime early = LocalDateTime.of(2026, 11, 10, 12, 0);
        LocalDateTime late = early.plusDays(5);
        when(client.getActiveCourses()).thenReturn(List.of(new CourseData("a", "Java"), new CourseData("b", "SQL")));
        when(client.getHomework("a")).thenReturn(List.of(
                new CourseWorkData("Без срока", "a1", null), new CourseWorkData("Раньше", "a2", early)));
        when(client.getHomework("b")).thenReturn(List.of(new CourseWorkData("Позже", "b1", late)));

        assertEquals(List.of(new HomeworkItem("SQL", "Позже", "b1", late),
                new HomeworkItem("Java", "Раньше", "a2", early),
                new HomeworkItem("Java", "Без срока", "a1", null)), service.getHomework());
    }

    @Test
    void keepsSourceOrderWhenDeadlinesAreEqual() {
        LocalDateTime due = LocalDateTime.of(2026, 11, 10, 12, 0);
        when(client.getActiveCourses()).thenReturn(List.of(new CourseData("a", "Java")));
        when(client.getHomework("a")).thenReturn(List.of(new CourseWorkData("Первое", "1", due),
                new CourseWorkData("Второе", "2", due), new CourseWorkData("Третье", "3", null)));

        assertEquals(List.of("Первое", "Второе", "Третье"),
                service.getHomework().stream().map(HomeworkItem::title).toList());
    }

    @Test
    void returnsEmptyHomeworkWhenThereAreNoCourses() {
        when(client.getActiveCourses()).thenReturn(List.of());
        assertTrue(service.getHomework().isEmpty());
        verify(client, never()).getHomework(anyString());
    }

    @Test
    void skipsEmptyCoursesWhileRetainingOtherCoursesHomework() {
        when(client.getActiveCourses()).thenReturn(List.of(new CourseData("a", "Java"), new CourseData("b", "SQL")));
        when(client.getHomework("a")).thenReturn(List.of());
        when(client.getHomework("b")).thenReturn(List.of(new CourseWorkData("Запрос", "url", null)));
        assertEquals(List.of(new HomeworkItem("SQL", "Запрос", "url", null)), service.getHomework());
    }

    @Test
    void propagatesCourseFailureInsteadOfReturningAPartialList() {
        ClassroomIntegrationException failure = new ClassroomIntegrationException("Second course failed", new IOException("Network"));
        when(client.getActiveCourses()).thenReturn(List.of(new CourseData("a", "Java"), new CourseData("b", "SQL")));
        when(client.getHomework("a")).thenReturn(List.of(new CourseWorkData("Лаба", "url", null)));
        when(client.getHomework("b")).thenThrow(failure);
        assertSame(failure, assertThrows(ClassroomIntegrationException.class, service::getHomework));
    }
}
