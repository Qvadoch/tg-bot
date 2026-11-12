package com.example.tg_bot.telegram.homework;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

/**
 * Создаёт клавиатуру переходов для просмотра заданий.
 */
@Component
public class HomeworkKeyboard {

    /**
     * Создаёт кнопки «Предыдущая» и «Следующая» с именами действий {@link PageAction}.
     * Обе кнопки присутствуют и на границах списка; допустимость перехода проверяет сервис.
     *
     * @return новая клавиатура с одной строкой из двух кнопок
     */
    public InlineKeyboardMarkup getInlineKeyboardMarkup() {
        InlineKeyboardButton previousPageButton = InlineKeyboardButton.builder()
                .text("Предыдущая")
                .callbackData(PageAction.PREVIOUS_PAGE.name())
                .build();
        InlineKeyboardButton nextPageButton = InlineKeyboardButton.builder()
                .text("Следующая")
                .callbackData(PageAction.NEXT_PAGE.name())
                .build();

        InlineKeyboardRow inlineKeyboardRow = new InlineKeyboardRow();
        inlineKeyboardRow.add(previousPageButton);
        inlineKeyboardRow.add(nextPageButton);

        return InlineKeyboardMarkup.builder().keyboardRow(inlineKeyboardRow).build();
    }
}
