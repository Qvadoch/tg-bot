package com.example.tg_bot.classroom.model;

import java.time.LocalDateTime;

/**
 * Работа курса после преобразования ответа Google API.
 *
 * @param title название работы
 * @param link ссылка на работу
 * @param due срок в зоне бота; {@code null}, если срок отсутствует
 */
public record CourseWorkData(String title, String link, LocalDateTime due) { }
