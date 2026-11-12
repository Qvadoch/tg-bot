package com.example.tg_bot.classroom.model;

/**
 * Минимальное представление курса, используемое приложением.
 *
 * @param id идентификатор курса во внешней системе
 * @param name название курса
 */
public record CourseData (String id, String name){ }
