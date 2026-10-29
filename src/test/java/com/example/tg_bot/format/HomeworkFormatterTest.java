package com.example.tg_bot.format;

import com.example.tg_bot.model.HomeworkItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeworkFormatterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 25, 12, 0);

    private final HomeworkFormatter formatter = new HomeworkFormatter();

    @Test
    @DisplayName("одно задание -> шапка, курс, название, срок и ссылка")
    void formatsSingleItem() {
        List<HomeworkItem> items = List.of(
                new HomeworkItem("Курсовая работа", "Курсач", "ссылка", LocalDateTime.of(2026, 10, 26, 14, 30)));

        String result = formatter.format(items, NOW);

        assertEquals("Задания (1):\n\n"
                + "Курсовая работа\n"
                + "Курсач\n"
                + "Срок: завтра в 14:30\n"
                + "ссылка", result);
    }

    @Test
    @DisplayName("задание без срока -> в тексте 'Без срока'")
    void formatsItemWithoutDue() {
        List<HomeworkItem> items = List.of(new HomeworkItem("Java", "Лаба 1", "ссылка", null));

        String result = formatter.format(items, NOW);

        assertTrue(result.contains("Без срока"));
    }

    @Test
    @DisplayName("12 заданий -> в шапке 12, а показано только 10")
    void limitsOutputToTenItems() {
        List<HomeworkItem> items = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            items.add(new HomeworkItem("Курс", "Задание " + i, "ссылка", null));
        }

        String result = formatter.format(items, NOW);

        assertTrue(result.startsWith("Задания (12):"));

        assertEquals(11, result.split("\n\n").length);
    }

    @Test
    @DisplayName("пустой список -> только шапка 'Задания (0):'")
    void formatsEmptyList() {
        String result = formatter.format(List.of(), NOW);

        assertEquals("Задания (0):\n\n", result);
    }
}