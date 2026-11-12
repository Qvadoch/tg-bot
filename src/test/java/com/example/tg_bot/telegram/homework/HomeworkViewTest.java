package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.homework.pagination.HomeworkPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HomeworkViewTest {
    @ParameterizedTest
    @ValueSource(ints = {-1, 2, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void rejectsIndexesOutsideTheRenderedParts(int index) {
        RenderedHomeworkPage rendered = new RenderedHomeworkPage(HomeworkPage.first(List.of()), List.of("one", "two"), null);
        assertThrows(IllegalArgumentException.class, () -> new HomeworkView(rendered, index));
    }

    @Test
    void selectsTheRequestedPartAndKeepsItsKeyboard() {
        InlineKeyboardMarkup keyboard = new HomeworkKeyboard().getInlineKeyboardMarkup();
        RenderedHomeworkPage rendered = new RenderedHomeworkPage(HomeworkPage.first(List.of()), List.of("one", "two"), keyboard);
        assertEquals("one", new HomeworkView(rendered, 0).text());
        HomeworkView second = new HomeworkView(rendered, 1);
        assertEquals("two", second.text());
        assertSame(keyboard, second.keyboard());
    }

    @Test
    void renderedPageSnapshotsItsPartsSoTheViewIndexRemainsValid() {
        List<String> parts = new ArrayList<>(List.of("one", "two"));
        RenderedHomeworkPage rendered = new RenderedHomeworkPage(HomeworkPage.first(List.of()), parts, null);
        HomeworkView view = new HomeworkView(rendered, 1);
        parts.clear();
        assertEquals("two", view.text());
        assertNull(view.keyboard());
        assertThrows(UnsupportedOperationException.class, () -> rendered.messageParts().clear());
    }

    @Test
    void rejectsARenderedPageWithoutAnyMessagePart() {
        assertThrows(IllegalArgumentException.class,
                () -> new RenderedHomeworkPage(HomeworkPage.first(List.of()), List.of(), null));
    }
}
