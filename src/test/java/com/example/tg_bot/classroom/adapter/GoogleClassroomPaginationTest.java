package com.example.tg_bot.classroom.adapter;

import com.example.tg_bot.classroom.converter.DueDateConverter;
import com.example.tg_bot.classroom.exception.ClassroomIntegrationException;
import com.example.tg_bot.classroom.model.CourseData;
import com.example.tg_bot.classroom.model.CourseWorkData;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleClassroomPaginationTest {
    private final Classroom google = mock(Classroom.class);
    private final Classroom.Courses courses = mock(Classroom.Courses.class);
    private final GoogleClassroomClient client = new GoogleClassroomClient(google, new DueDateConverter(ZoneOffset.UTC));

    @Test
    void combinesNonEmptyCoursePagesAndStopsAtAnEmptyNextToken() throws IOException {
        Classroom.Courses.List request = mock(Classroom.Courses.List.class, RETURNS_SELF);
        when(google.courses()).thenReturn(courses);
        when(courses.list()).thenReturn(request);
        when(request.execute()).thenReturn(
                new ListCoursesResponse().setCourses(List.of(new Course().setId("a").setName("Java"))).setNextPageToken("next"),
                new ListCoursesResponse().setCourses(List.of(new Course().setId("b").setName("SQL"))).setNextPageToken(""));

        assertEquals(List.of(new CourseData("a", "Java"), new CourseData("b", "SQL")), client.getActiveCourses());
        verify(request, times(2)).execute();
        verify(request).setPageToken("next");
    }

    @Test
    void failsTheWholeCourseLoadIfALaterPageFails() throws IOException {
        Classroom.Courses.List request = mock(Classroom.Courses.List.class, RETURNS_SELF);
        when(google.courses()).thenReturn(courses);
        when(courses.list()).thenReturn(request);
        IOException failure = new IOException("Second page failed");
        when(request.execute()).thenReturn(new ListCoursesResponse()
                .setCourses(List.of(new Course().setId("a").setName("Java"))).setNextPageToken("next"))
                .thenThrow(failure);

        assertSame(failure, assertThrows(ClassroomIntegrationException.class, client::getActiveCourses).getCause());
    }

    @Test
    void combinesCourseworkAcrossPagesAndConvertsDates() throws IOException {
        Classroom.Courses.CourseWork courseWork = mock(Classroom.Courses.CourseWork.class);
        Classroom.Courses.CourseWork.List request = mock(Classroom.Courses.CourseWork.List.class, RETURNS_SELF);
        when(google.courses()).thenReturn(courses);
        when(courses.courseWork()).thenReturn(courseWork);
        when(courseWork.list("a")).thenReturn(request);
        CourseWork first = new CourseWork().setTitle("Первое").setAlternateLink("url1")
                .setDueDate(new Date().setYear(2026).setMonth(11).setDay(11))
                .setDueTime(new TimeOfDay().setHours(12).setMinutes(30));
        when(request.execute()).thenReturn(new ListCourseWorkResponse().setCourseWork(List.of(first)).setNextPageToken("next"),
                new ListCourseWorkResponse().setCourseWork(List.of(new CourseWork().setTitle("Второе").setAlternateLink("url2"))));

        assertEquals(List.of(new CourseWorkData("Первое", "url1", LocalDateTime.of(2026, 11, 11, 12, 30)),
                new CourseWorkData("Второе", "url2", null)), client.getHomework("a"));
        verify(request).setPageToken("next");
    }

    @Test
    void returnsEmptyCourseworkForAResponseWithoutItemsOrNextToken() throws IOException {
        Classroom.Courses.CourseWork courseWork = mock(Classroom.Courses.CourseWork.class);
        Classroom.Courses.CourseWork.List request = mock(Classroom.Courses.CourseWork.List.class, RETURNS_SELF);
        when(google.courses()).thenReturn(courses);
        when(courses.courseWork()).thenReturn(courseWork);
        when(courseWork.list("a")).thenReturn(request);
        when(request.execute()).thenReturn(new ListCourseWorkResponse());
        assertTrue(client.getHomework("a").isEmpty());
        verify(request, times(1)).execute();
    }

    @Test
    void readsPastAnEmptyMiddleCourseworkPageAndStopsAtAnEmptyToken() throws IOException {
        Classroom.Courses.CourseWork courseWork = mock(Classroom.Courses.CourseWork.class);
        Classroom.Courses.CourseWork.List request = mock(Classroom.Courses.CourseWork.List.class, RETURNS_SELF);
        when(google.courses()).thenReturn(courses);
        when(courses.courseWork()).thenReturn(courseWork);
        when(courseWork.list("a")).thenReturn(request);
        when(request.execute()).thenReturn(
                new ListCourseWorkResponse().setCourseWork(List.of(new CourseWork().setTitle("First")))
                        .setNextPageToken("middle"),
                new ListCourseWorkResponse().setCourseWork(List.of()).setNextPageToken("last"),
                new ListCourseWorkResponse().setCourseWork(List.of(new CourseWork().setTitle("Last")))
                        .setNextPageToken(""));

        assertEquals(List.of(new CourseWorkData("First", null, null), new CourseWorkData("Last", null, null)),
                client.getHomework("a"));
        verify(request, times(3)).execute();
        verify(request).setPageToken("middle");
        verify(request).setPageToken("last");
    }

    @Test
    void doesNotReturnPartialHomeworkWhenALaterPageFails() throws IOException {
        Classroom.Courses.CourseWork courseWork = mock(Classroom.Courses.CourseWork.class);
        Classroom.Courses.CourseWork.List request = mock(Classroom.Courses.CourseWork.List.class, RETURNS_SELF);
        when(google.courses()).thenReturn(courses);
        when(courses.courseWork()).thenReturn(courseWork);
        when(courseWork.list("a")).thenReturn(request);
        IOException failure = new IOException("Later coursework page failed");
        when(request.execute()).thenReturn(new ListCourseWorkResponse()
                .setCourseWork(List.of(new CourseWork().setTitle("Partial result"))).setNextPageToken("next"))
                .thenThrow(failure);

        ClassroomIntegrationException error = assertThrows(ClassroomIntegrationException.class,
                () -> client.getHomework("a"));

        assertSame(failure, error.getCause());
        assertTrue(error.getMessage().contains("a"));
        verify(request, times(2)).execute();
    }
}
