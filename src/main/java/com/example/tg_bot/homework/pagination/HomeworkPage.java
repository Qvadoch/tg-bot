package com.example.tg_bot.homework.pagination;

import com.example.tg_bot.homework.model.HomeworkItem;

import java.util.List;

/**
 * Неизменяемое положение в снимке заданий: полный список и начало выбранной десятки.
 * Переходы используют тот же снимок и сохраняют прежнюю страницу без изменений.
 */
public final class HomeworkPage {
    private static final int PAGE_SIZE = 10;
    private final List<HomeworkItem> homework;
    private final int start;

    private HomeworkPage(List<HomeworkItem> homework, int start) {
        this.homework = homework;
        this.start = start;
    }

    /**
     * Создаёт первую страницу, копируя состав и порядок входного списка.
     *
     * @param homework полный список заданий; может быть пустым
     * @return первая страница со снимком списка
     * @throws NullPointerException если список или любой его элемент равен {@code null}
     */
    public static HomeworkPage first(List<HomeworkItem> homework) {
        return new HomeworkPage(List.copyOf(homework), 0);
    }

    /**
     * Возвращает задания текущей страницы в исходном порядке.
     *
     * @return неизменяемый список не более десяти элементов; пустой при отсутствии заданий
     */
    public List<HomeworkItem> items() {
        int count = Math.min(PAGE_SIZE, totalCount() - start);
        return homework.subList(start, start + count);
    }

    /**
     * Возвращает размер полного снимка, включая задания других страниц.
     *
     * @return общее количество заданий
     */
    public int totalCount() {
        return homework.size();
    }

    /**
     * Подготавливает следующую страницу без изменения текущей.
     *
     * @return новая страница либо {@code this}, если следующей страницы нет
     */
    public HomeworkPage next() {
        if (totalCount() - start <= PAGE_SIZE) {
            return this;
        }
        return new HomeworkPage(homework, start + PAGE_SIZE);
    }

    /**
     * Подготавливает предыдущую страницу без изменения текущей.
     *
     * @return новая страница либо {@code this}, если текущая страница первая
     */
    public HomeworkPage previous() {
        if (start == 0) {
            return this;
        }
        return new HomeworkPage(homework, start - PAGE_SIZE);
    }
}
