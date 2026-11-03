package com.example.tg_bot.classroom.adapter;

import com.example.tg_bot.classroom.port.ClassroomClient;
import com.example.tg_bot.classroom.converter.DueDateConverter;
import com.example.tg_bot.classroom.exception.ClassroomIntegrationException;
import com.example.tg_bot.classroom.model.CourseData;
import com.example.tg_bot.classroom.model.CourseWorkData;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.ListCourseWorkResponse;
import com.google.api.services.classroom.model.ListCoursesResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class GoogleClassroomClient implements ClassroomClient {

    private final Classroom classroom;
    private final DueDateConverter dueDateConverter;

    public GoogleClassroomClient(Classroom classroom, DueDateConverter dueDateConverter) {
        this.classroom = classroom;
        this.dueDateConverter = dueDateConverter;
    }

    @Override
    public List<CourseData> getActiveCourses() {
        ListCoursesResponse response;

        try {
            response = classroom.courses().list()
                    .setCourseStates(List.of("ACTIVE"))
                    .execute();
        } catch (IOException e) {
            throw new ClassroomIntegrationException("Failed to load courses from Google Classroom", e);
        }

        List<CourseData> allCourses = new ArrayList<>();
        String pageToken;

        do {
            List<Course> courses = response.getCourses() != null ? response.getCourses() : List.of();
            pageToken = response.getNextPageToken();

            for (Course course : courses) {
                String courseId = course.getId();
                String courseName = course.getName();

                allCourses.add(new CourseData(courseId, courseName));
            }


            if (pageToken != null) {
                try {
                    response = classroom.courses()
                            .list()
                            .setCourseStates(List.of("ACTIVE"))
                            .setPageToken(pageToken)
                            .execute();
                } catch (IOException e) {
                    throw new ClassroomIntegrationException("Failed to load next page of courses", e);
                }
            }

        } while (pageToken != null);

        return allCourses;
    }

    @Override
    public List<CourseWorkData> getHomework(String courseId) {
        ListCourseWorkResponse response;

        try {
            response = classroom.courses().courseWork()
                    .list(courseId)
                    .execute();
        } catch (IOException e) {
            throw new ClassroomIntegrationException("Failed to load courseWorks from Google Classroom", e);
        }

        List<CourseWorkData> allCourseWorks = new ArrayList<>();
        String pageToken;

        do {
            List<CourseWork> courseWorks = response.getCourseWork() != null ? response.getCourseWork() : List.of();
            pageToken = response.getNextPageToken();


            for (CourseWork courseWork : courseWorks) {
                String title = courseWork.getTitle();
                String link = courseWork.getAlternateLink();
                LocalDateTime due = dueDateConverter.getDue(courseWork.getDueDate(), courseWork.getDueTime());

                allCourseWorks.add(new CourseWorkData(title, link, due));
            }

            if (pageToken != null) {
                try {
                    response = classroom.courses().courseWork()
                            .list(courseId)
                            .setPageToken(pageToken)
                            .execute();
                } catch (IOException e) {
                    throw new ClassroomIntegrationException("Failed to load next page of courseWorks", e);
                }
            }

        } while (pageToken != null);

        return allCourseWorks;
    }
}
