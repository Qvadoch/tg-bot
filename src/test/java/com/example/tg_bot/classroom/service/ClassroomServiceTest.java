package com.example.tg_bot.classroom.service;

import com.example.tg_bot.classroom.adapter.GoogleClassroomClient;
import com.example.tg_bot.classroom.converter.DueDateConverter;
import com.example.tg_bot.classroom.exception.ClassroomIntegrationException;
import com.example.tg_bot.classroom.model.CourseData;
import com.example.tg_bot.homework.model.HomeworkItem;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.services.classroom.Classroom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassroomServiceTest {

    private static ClassroomService service(String coursesJson, String coursesWorkJson, int status) {
        MockHttpTransport httpTransport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() {
                        String body = url.contains("courseWork") ? coursesWorkJson : coursesJson;
                        return new MockLowLevelHttpResponse()
                                .setStatusCode(status)
                                .setContentType("application/json")
                                .setContent(body);
                    }
                };
            }
        };

        Classroom classroom = new Classroom.Builder(httpTransport, GsonFactory.getDefaultInstance(), null)
                .setApplicationName("tg_bot")
                .build();

        DueDateConverter dueDateConverter = new DueDateConverter(ZoneId.of("Europe/Moscow"));

        return new ClassroomService(new GoogleClassroomClient(classroom, dueDateConverter));
    }

    @Test
    @DisplayName("getActiveCourses -> получает ID и названия курсов через адаптер")
    void getActiveCoursesReturnsCourseData() {
        String coursesJson = """
                {
                  "courses": [
                    {"id": "1", "name": "Java"},
                    {"id": "2", "name": "SQL"}
                  ]
                }
                """;

        ClassroomService service = service(coursesJson, null, 200);

        List<CourseData> courses = service.getActiveCourses();

        assertEquals(List.of(new CourseData("1", "Java"), new CourseData("2", "SQL")), courses);
    }

    @Test
    @DisplayName("getActiveCourses -> ответ без курсов -> пустой список")
    void getActiveCoursesReturnsEmptyWhenNoCourses() {
        String coursesJson = "{}";

        ClassroomService service = service(coursesJson, null, 200);

        List<CourseData> courses = service.getActiveCourses();

        assertTrue(courses.isEmpty());
    }

    @Test
    @DisplayName("getActiveCourses -> статус 500 -> ClassroomIntegrationException с исходной причиной")
    void getActiveCoursesThrowsOnServerError() {
        ClassroomService service = service("{}", null, 500);

        ClassroomIntegrationException exception = assertThrows(
                ClassroomIntegrationException.class, service::getActiveCourses);

        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    @DisplayName("getHomework -> получает задания через адаптер и добавляет название курса")
    void getHomeworkIncludesCourseName() {
        String coursesJson = """
                {
                  "courses": [
                    {"id": "1", "name": "Java"}
                  ]
                }
                """;
        String courseWorkJson = """
                {
                  "courseWork": [
                    {
                      "title": "Лаба 1",
                      "alternateLink": "https://classroom.google.com/...",
                      "dueDate": {"year": 2026, "month": 10, "day": 30},
                      "dueTime": {"hours": 14, "minutes": 30}
                    }
                  ]
                }
                """;

        ClassroomService service = service(coursesJson, courseWorkJson, 200);

        List<HomeworkItem> homework = service.getHomework();

        assertEquals(1, homework.size());
        assertEquals("Java", homework.get(0).courseName());
        assertEquals("Лаба 1", homework.get(0).title());
    }

    @Test
    @DisplayName("getHomework -> конвертирует время UTC -> Moscow")
    void getHomeworkConvertsUtcToMoscow() {
        String coursesJson = """
                {
                  "courses": [
                    {"id": "1", "name": "Java"}
                  ]
                }
                """;
        String courseWorkJson = """
                {
                  "courseWork": [
                    {
                      "title": "Лаба",
                      "alternateLink": "link",
                      "dueDate": {"year": 2026, "month": 10, "day": 30},
                      "dueTime": {"hours": 14, "minutes": 30}
                    }
                  ]
                }
                """;

        ClassroomService service = service(coursesJson, courseWorkJson, 200);

        List<HomeworkItem> homework = service.getHomework();

        assertEquals(LocalDateTime.of(2026, 10, 30, 17, 30), homework.get(0).due());
    }

    @Test
    @DisplayName("getHomework -> задание без времени -> 23:59")
    void getHomeworkUsesEndOfDayWhenNoTime() {
        String coursesJson = """
                {
                  "courses": [
                    {"id": "1", "name": "Java"}
                  ]
                }
                """;
        String courseWorkJson = """
                {
                  "courseWork": [
                    {
                      "title": "Лаба",
                      "alternateLink": "link",
                      "dueDate": {"year": 2026, "month": 10, "day": 30}
                    }
                  ]
                }
                """;

        ClassroomService service = service(coursesJson, courseWorkJson, 200);

        List<HomeworkItem> homework = service.getHomework();

        assertEquals(LocalDateTime.of(2026, 10, 30, 23, 59), homework.get(0).due());
    }
}
