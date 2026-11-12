package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.homework.pagination.HomeworkPage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

/**
 * Результат подготовки страницы: исходные задания, все части текста и клавиатура.
 * Список частей копируется при создании; клавиатура хранится как переданный объект.
 *
 * @param page исходная страница заданий
 * @param messageParts непустой список частей текста в порядке показа
 * @param keyboard клавиатура; {@code null} для ответа без кнопок
 */
record RenderedHomeworkPage(HomeworkPage page, List<String> messageParts, InlineKeyboardMarkup keyboard) {
    /**
     * Фиксирует состав частей и проверяет возможность показать хотя бы одну часть.
     *
     * @throws IllegalArgumentException если список частей пуст
     * @throws NullPointerException если список частей или его элемент равен {@code null}
     */
    RenderedHomeworkPage {
        messageParts = List.copyOf(messageParts);
        if (messageParts.isEmpty()) {
            throw new IllegalArgumentException("A rendered homework page must contain at least one message part");
        }
    }
}
