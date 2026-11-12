package com.example.tg_bot.homework.pagination;

import com.example.tg_bot.homework.model.HomeworkItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class HomeworkPageTest {
    @Test
    void traversesPagesWithoutChangingThePreviousPage() {
        List<HomeworkItem> items = items(21);
        HomeworkPage first = HomeworkPage.first(items);
        HomeworkPage second = first.next();
        HomeworkPage last = second.next();

        assertSame(first, first.previous());
        assertSame(last, last.next());
        assertEquals(items.subList(0, 10), first.items());
        assertEquals(items.subList(10, 20), second.items());
        assertEquals(items.subList(20, 21), last.items());
        assertEquals(second.items(), last.previous().items());
        assertEquals(21, last.totalCount());
    }

    @Test
    void snapshotsTheListWhenOpeningANewView() {
        List<HomeworkItem> original = items(12);
        List<HomeworkItem> mutable = new ArrayList<>(original);
        HomeworkPage page = HomeworkPage.first(mutable);
        mutable.clear();
        assertEquals(original.subList(10, 12), page.next().items());
    }

    @Test
    void emptyAndExactlyFullPagesHaveNoNextPage() {
        HomeworkPage empty = HomeworkPage.first(List.of());
        HomeworkPage full = HomeworkPage.first(items(10));
        assertTrue(empty.items().isEmpty());
        assertSame(empty, empty.next());
        assertSame(full, full.next());
    }

    private List<HomeworkItem> items(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new HomeworkItem("Java", "Задание " + i, "url", null))
                .toList();
    }
}
