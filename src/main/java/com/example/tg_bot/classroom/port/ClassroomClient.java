package com.example.tg_bot.classroom.port;

import com.example.tg_bot.classroom.model.CourseData;
import com.example.tg_bot.classroom.model.CourseWorkData;

import java.util.List;

public interface ClassroomClient {
    List<CourseData> getActiveCourses();
    List<CourseWorkData> getHomework(String courseId);
}
