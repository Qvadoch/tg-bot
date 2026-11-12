package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.homework.formatter.HomeworkFormatter;
import com.example.tg_bot.homework.model.HomeworkItem;
import com.example.tg_bot.homework.pagination.HomeworkPage;
import com.example.tg_bot.telegram.TelegramTextSplitter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HomeworkPageRendererTest {
    @Test
    void rendersAllPartsWithoutLosingTextOrBreakingEmoji() {
        Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        HomeworkFormatter formatter = new HomeworkFormatter();
        HomeworkPageRenderer renderer = new HomeworkPageRenderer(formatter, new HomeworkKeyboard(), clock, new TelegramTextSplitter());
        String header = "Задания (11):\n\nJava\n";
        String title = "x".repeat(4095 - header.length()) + "📗" + "y".repeat(500);
        List<HomeworkItem> items = new ArrayList<>();
        items.add(new HomeworkItem("Java", title, "https://example.com/1", null));
        for (int i = 2; i <= 11; i++) {
            items.add(new HomeworkItem("Java", "Работа " + i, "https://example.com/" + i, null));
        }

        HomeworkPage page = HomeworkPage.first(items);
        RenderedHomeworkPage rendered = renderer.render(page);
        RenderedHomeworkPage last = renderer.render(page.next());

        assertEquals(formatter.format(page.items(), LocalDateTime.now(clock), 11), String.join("", rendered.messageParts()));
        List<String> allParts = new ArrayList<>(rendered.messageParts());
        allParts.addAll(last.messageParts());
        for (String text : allParts) {
            assertTrue(text.length() <= 4096);
            assertFalse(Character.isHighSurrogate(text.charAt(text.length() - 1)));
            assertFalse(Character.isLowSurrogate(text.charAt(0)));
        }
        assertTrue(last.messageParts().getFirst().contains("Работа 11"));
    }

    @Test
    void rendersEmptyHomeworkWithoutButtons() {
        HomeworkPageRenderer renderer = new HomeworkPageRenderer(new HomeworkFormatter(), new HomeworkKeyboard(),
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), new TelegramTextSplitter());

        RenderedHomeworkPage rendered = renderer.render(HomeworkPage.first(List.of()));

        assertEquals(List.of("Заданий пока нет."), rendered.messageParts());
        assertNull(rendered.keyboard());
    }
}
