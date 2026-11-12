package com.example.tg_bot.homework.model;

import java.time.LocalDateTime;

/**
 * Задание для показа пользователю, объединённое с названием курса.
 *
 * @param courseName название курса
 * @param title название работы
 * @param link ссылка на работу
 * @param due срок в зоне бота; {@code null}, если срок не задан
 */
public record HomeworkItem(String courseName, String title, String link, LocalDateTime due) { }
