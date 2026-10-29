package com.example.tg_bot.google.classroom;

import com.example.tg_bot.model.HomeworkItem;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ClassroomService {
    private final Classroom classroom;
    private final DueDateConverter dueDateConverter;

    public ClassroomService(Classroom classroom, DueDateConverter dueDateConverter) {
        this.classroom = classroom;
        this.dueDateConverter = dueDateConverter;
    }

    public List<String> getCourses() throws IOException {
        return getActiveCourses().stream()
                .map(Course::getName)
                .toList();
    }

    public List<HomeworkItem> getHomework() throws IOException {
        List<HomeworkItem> result = new ArrayList<>();
        for (Course course : getActiveCourses()) {
            List<CourseWork> works = classroom.courses().courseWork()
                    .list(course.getId())
                    .execute()
                    .getCourseWork();

            if (works == null) {
                continue;
            }

            List<HomeworkItem> items = works.stream()
                    .map(w -> new HomeworkItem(course.getName(), w.getTitle(), w.getAlternateLink(), dueDateConverter.getDue(w.getDueDate(), w.getDueTime())))
                    .toList();

            result.addAll(items);
        }
        return result.stream()
                .sorted(Comparator.comparing(HomeworkItem::due, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<Course> getActiveCourses() throws IOException {
        List<Course> courses = classroom.courses().list()
                .setCourseStates(List.of("ACTIVE"))
                .execute()
                .getCourses();
        return courses != null ? courses : List.of();
    }
}