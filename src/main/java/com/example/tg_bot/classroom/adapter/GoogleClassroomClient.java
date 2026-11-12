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

/**
 * Адаптер Google Classroom API для контракта {@link ClassroomClient}.
 * Загружает все страницы ответов, преобразует модели Google и сохраняет исходную
 * причину ошибки в {@link ClassroomIntegrationException}.
 *
 * <p>Пустая страница с токеном продолжения не завершает загрузку.
 * Ошибка любой страницы прерывает операцию без возврата частичного результата.
 */
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
        List<CourseData> courses = new ArrayList<>();
        try {
            Classroom.Courses.List request = classroom.courses().list().setCourseStates(List.of("ACTIVE"));
            String pageToken = null;
            do {
                ListCoursesResponse page = request.setPageToken(pageToken).execute();
                if (page.getCourses() != null) {
                    for (Course course : page.getCourses()) {
                        courses.add(new CourseData(course.getId(), course.getName()));
                    }
                }
                pageToken = page.getNextPageToken();
            } while (pageToken != null && !pageToken.isEmpty());
        } catch (IOException exception) {
            throw new ClassroomIntegrationException("Failed to load active courses from Google Classroom", exception);
        }
        return courses;
    }

    @Override
    public List<CourseWorkData> getHomework(String courseId) {
        List<CourseWorkData> homework = new ArrayList<>();
        try {
            Classroom.Courses.CourseWork.List request = classroom.courses().courseWork().list(courseId);
            String pageToken = null;
            do {
                ListCourseWorkResponse page = request.setPageToken(pageToken).execute();
                if (page.getCourseWork() != null) {
                    for (CourseWork work : page.getCourseWork()) {
                        LocalDateTime due = dueDateConverter.getDue(work.getDueDate(), work.getDueTime());
                        homework.add(new CourseWorkData(work.getTitle(), work.getAlternateLink(), due));
                    }
                }
                pageToken = page.getNextPageToken();
            } while (pageToken != null && !pageToken.isEmpty());
        } catch (IOException exception) {
            throw new ClassroomIntegrationException("Failed to load coursework for Google Classroom course " + courseId, exception);
        }
        return homework;
    }
}
