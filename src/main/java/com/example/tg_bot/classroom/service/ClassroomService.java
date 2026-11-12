package com.example.tg_bot.classroom.service;

import com.example.tg_bot.classroom.port.ClassroomClient;
import com.example.tg_bot.classroom.model.CourseData;
import com.example.tg_bot.classroom.model.CourseWorkData;
import com.example.tg_bot.homework.model.HomeworkItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Собирает работы активных курсов в общий список заданий с названиями курсов.
 * Использует контракт клиента, поэтому правила сборки не зависят от Google SDK.
 */
@Service
public class ClassroomService {
    private final ClassroomClient classroomClient;

    public ClassroomService(ClassroomClient classroomClient) {
        this.classroomClient = classroomClient;
    }

    /**
     * Передаёт запрос активных курсов клиенту Classroom.
     *
     * @return доступные активные курсы
     * @throws com.example.tg_bot.classroom.exception.ClassroomIntegrationException при ошибке загрузки
     */
    public List<CourseData> getActiveCourses() {
        return classroomClient.getActiveCourses();
    }

    /**
     * Загружает работы всех активных курсов и добавляет к каждой название курса.
     * Сортирует сроки по убыванию, помещая работы без срока в конец.
     * Работы с одинаковыми сроками сохраняют порядок поступления.
     *
     * <p>Загрузка курсов выполняется последовательно. Ошибка любого курса прерывает
     * операцию; частичный список не возвращается. Факт сдачи работы не проверяется.
     *
     * @return неизменяемый список заданий, возможно пустой
     * @throws com.example.tg_bot.classroom.exception.ClassroomIntegrationException при ошибке загрузки
     */
    public List<HomeworkItem> getHomework() {
        List<CourseData> activeCourses = classroomClient.getActiveCourses();

        List<HomeworkItem> result = new ArrayList<>();
        for (CourseData courseData : activeCourses) {
            for (CourseWorkData work : classroomClient.getHomework(courseData.id())) {
                result.add(new HomeworkItem(courseData.name(), work.title(), work.link(), work.due()));
            }
        }
        return result.stream()
                .sorted(Comparator.comparing(HomeworkItem::due, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

}
