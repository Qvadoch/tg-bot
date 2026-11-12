package com.example.tg_bot.classroom.port;

import com.example.tg_bot.classroom.model.CourseData;
import com.example.tg_bot.classroom.model.CourseWorkData;

import java.util.List;

/**
 * Контракт чтения курсов и работ, независимый от типов Google SDK.
 * Реализация преобразует ответы внешней системы в модели приложения.
 */
public interface ClassroomClient {
    /**
     * Возвращает активные курсы подключённого аккаунта.
     *
     * @return список курсов; пустой, если активных курсов нет
     * @throws com.example.tg_bot.classroom.exception.ClassroomIntegrationException при ошибке загрузки
     */
    List<CourseData> getActiveCourses();
    /**
     * Возвращает работы указанного курса. Контракт не фильтрует работы по факту сдачи.
     *
     * @param courseId идентификатор курса во внешней системе
     * @return список работ; пустой, если работ нет
     * @throws com.example.tg_bot.classroom.exception.ClassroomIntegrationException при ошибке загрузки
     */
    List<CourseWorkData> getHomework(String courseId);
}
