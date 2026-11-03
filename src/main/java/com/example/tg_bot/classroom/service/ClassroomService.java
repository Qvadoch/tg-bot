package com.example.tg_bot.classroom.service;

import com.example.tg_bot.classroom.port.ClassroomClient;
import com.example.tg_bot.classroom.model.CourseData;
import com.example.tg_bot.classroom.model.CourseWorkData;
import com.example.tg_bot.homework.model.HomeworkItem;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassroomService {
    private final ClassroomClient classroomClient;

    public ClassroomService(ClassroomClient classroomClient) {
        this.classroomClient = classroomClient;
    }

    public List<HomeworkItem> getHomework(){
        List<CourseData> activeCourses = classroomClient.getActiveCourses();

        List<HomeworkItem> result = new ArrayList<>();
        for (CourseData courseData : activeCourses) {
            String courseId = courseData.id();
            String courseName = courseData.name();
            List<CourseWorkData> courseWorkData = classroomClient.getHomework(courseId);

            for (CourseWorkData courseWork : courseWorkData) {
                String courseWorkTitle = courseWork.title();
                String courseWorkLink = courseWork.link();
                LocalDateTime courseWorkDue = courseWork.due();

                result.add(new HomeworkItem(courseName, courseWorkTitle, courseWorkLink, courseWorkDue));
            }

        }
        return result.stream()
                .sorted(Comparator.comparing(HomeworkItem::due, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

}