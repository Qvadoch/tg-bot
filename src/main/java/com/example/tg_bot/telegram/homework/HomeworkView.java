package com.example.tg_bot.telegram.homework;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Выбранная часть текста подготовленной страницы.
 * Индекс относится к частям сообщения, а положение в списке заданий хранится в странице.
 *
 * @param renderedPage подготовленная страница с текстом и клавиатурой
 * @param messagePartIndex индекс выбранной части, начиная с нуля
 */
record HomeworkView(RenderedHomeworkPage renderedPage, int messagePartIndex) {
    /**
     * Проверяет, что выбранная часть существует.
     *
     * @throws IllegalArgumentException если индекс отрицателен или выходит за границу списка
     */
    HomeworkView {
        if (messagePartIndex < 0 || messagePartIndex >= renderedPage.messageParts().size()) {
            throw new IllegalArgumentException("Message part index is outside the rendered homework page");
        }
    }

    /**
     * Возвращает текст выбранной части.
     *
     * @return одна подготовленная строка для отправки или редактирования
     */
    String text() {
        return renderedPage.messageParts().get(messagePartIndex);
    }

    /**
     * Возвращает клавиатуру подготовленной страницы.
     *
     * @return клавиатура либо {@code null}, если кнопки не требуются
     */
    InlineKeyboardMarkup keyboard() {
        return renderedPage.keyboard();
    }
}
