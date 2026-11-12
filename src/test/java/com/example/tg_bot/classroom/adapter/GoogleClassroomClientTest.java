package com.example.tg_bot.classroom.adapter;

import com.example.tg_bot.classroom.converter.DueDateConverter;
import com.example.tg_bot.classroom.exception.ClassroomIntegrationException;
import com.example.tg_bot.classroom.model.CourseData;
import com.example.tg_bot.classroom.model.CourseWorkData;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleClassroomClientTest {
    private final Classroom classroom = mock(Classroom.class);
    private final Classroom.Courses courses = mock(Classroom.Courses.class);
    private final GoogleClassroomClient client = new GoogleClassroomClient(classroom, new DueDateConverter(ZoneOffset.UTC));

    @Test
    void readsActiveCoursesAcrossAnEmptyPage() throws IOException {
        Classroom.Courses.List request = mock(Classroom.Courses.List.class, RETURNS_SELF);
        when(classroom.courses()).thenReturn(courses);
        when(courses.list()).thenReturn(request);
        when(request.execute()).thenReturn(new ListCoursesResponse().setNextPageToken("second"),
                new ListCoursesResponse().setCourses(List.of(new Course().setId("abc").setName("Java"))));

        assertEquals(List.of(new CourseData("abc", "Java")), client.getActiveCourses());
        verify(request).setCourseStates(List.of("ACTIVE"));
        verify(request).setPageToken("second");
    }

    @Test
    void readsAllCourseworkAndPreservesTheCauseOfPageFailures() throws IOException {
        Classroom.Courses.CourseWork courseWork = mock(Classroom.Courses.CourseWork.class);
        Classroom.Courses.CourseWork.List request = mock(Classroom.Courses.CourseWork.List.class, RETURNS_SELF);
        when(classroom.courses()).thenReturn(courses);
        when(courses.courseWork()).thenReturn(courseWork);
        when(courseWork.list("abc")).thenReturn(request);
        ListCourseWorkResponse page = new ListCourseWorkResponse().setCourseWork(List.of(new CourseWork().setTitle("Java")));
        IOException failure = new IOException("network");
        when(request.execute()).thenReturn(new ListCourseWorkResponse().setNextPageToken("next"), page)
                .thenThrow(failure);

        assertEquals(List.of(new CourseWorkData("Java", null, null)), client.getHomework("abc"));
        verify(request).setPageToken("next");
        ClassroomIntegrationException error = assertThrows(ClassroomIntegrationException.class, () -> client.getHomework("abc"));
        assertEquals("Failed to load coursework for Google Classroom course abc", error.getMessage());
        assertSame(failure, error.getCause());
    }
}
