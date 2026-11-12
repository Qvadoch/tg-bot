package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.homework.formatter.HomeworkFormatter;
import com.example.tg_bot.homework.pagination.HomeworkPage;
import com.example.tg_bot.telegram.TelegramTextSplitter;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Подготавливает страницу для показа: форматирует задания, делит текст и создаёт кнопки.
 * Время передаётся через {@link Clock}, поэтому результат можно проверять с фиксированными часами.
 */
@Component
class HomeworkPageRenderer {
    private final HomeworkFormatter formatter;
    private final HomeworkKeyboard keyboard;
    private final Clock clock;
    private final TelegramTextSplitter textSplitter;

    HomeworkPageRenderer(HomeworkFormatter formatter, HomeworkKeyboard keyboard, Clock clock,
                         TelegramTextSplitter textSplitter) {
        this.formatter = formatter;
        this.keyboard = keyboard;
        this.clock = clock;
        this.textSplitter = textSplitter;
    }

    /**
     * Подготавливает все части одной страницы с временем форматирования на момент вызова.
     * Для пустого снимка формирует ответ «Заданий пока нет.» без кнопок.
     *
     * @param page выбранная страница снимка заданий
     * @return исходная страница вместе с подготовленными частями текста и клавиатурой
     */
    RenderedHomeworkPage render(HomeworkPage page) {
        if (page.totalCount() == 0) {
            return new RenderedHomeworkPage(page, List.of("Заданий пока нет."), null);
        }
        String text = formatter.format(page.items(), LocalDateTime.now(clock), page.totalCount());
        return new RenderedHomeworkPage(page, textSplitter.split(text), keyboard.getInlineKeyboardMarkup());
    }
}
