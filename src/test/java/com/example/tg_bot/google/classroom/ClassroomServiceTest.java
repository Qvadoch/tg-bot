package com.example.tg_bot.google.classroom;

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.client.util.Preconditions;
import com.google.api.services.classroom.Classroom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClassroomServiceTest {

    private static ClassroomService service (String coursesJson, String coursesWorkJson, int status){
        MockHttpTransport httpTransport = new MockHttpTransport(){
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url){
                return new MockLowLevelHttpRequest(){
                    @Override
                    public LowLevelHttpResponse execute(){
                        String body = url.contains("courses") ? coursesJson : coursesWorkJson;
                        return new MockLowLevelHttpResponse()
                                .setStatusCode(status)
                                .setContentType("application/json")
                                .setContent(body);
                    }
                };
            }
        };

        Classroom classroom = new Classroom.Builder(httpTransport, GsonFactory.getDefaultInstance(),  null)
                .setApplicationName("tg_bot")
                .build();

        DueDateConverter dueDateConverter = new DueDateConverter(ZoneId.of("Europe/Moscow"));

        return new ClassroomService(classroom, dueDateConverter);
    }

    @Test
    @DisplayName("getCourses -> парсит JSON и возвращает список названий")
    void getCoursesParsesJson() throws IOException {
        String coursesJson = """
        {
          "courses": [
            {"id": "1", "name": "Java"},
            {"id": "2", "name": "SQL"}
          ]
        }
        """;

        ClassroomService service = service(coursesJson, null, 200);

        List<String> courses = service.getCourses();

        assertEquals(List.of("Java", "SQL"), courses);
    }



    }