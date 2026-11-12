package com.example.tg_bot.telegram;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Подготавливает части текста для сообщений Telegram.
 * Длина измеряется в единицах UTF-16, как у {@link String#length()}.
 */
@Component
public class TelegramTextSplitter {
    private static final int MAX_MESSAGE_LENGTH = 4096;

    /**
     * Делит текст на части длиной не более 4096 единиц UTF-16.
     * Короткий текст возвращается единственной частью, включая пустую строку.
     *
     * <p>Для длинного текста предпочитает последний перенос строки перед границей,
     * если часть после такого разреза длиннее половины лимита. Перенос включается в часть.
     * При разрезе по длине сохраняет целостность корректной суррогатной пары.
     * Это не гарантирует целостность составных эмодзи из нескольких Unicode-символов.
     *
     * <p>Полностью пробельные части длинного текста пропускаются по {@link String#isBlank()}.
     *
     * @param text исходный текст; не должен быть {@code null}
     * @return неизменяемый список частей в исходном порядке; может быть пустым для длинного пробельного текста
     * @throws NullPointerException если {@code text} равен {@code null}
     */
    public List<String> split(String text) {
        if (text.length() <= MAX_MESSAGE_LENGTH) {
            return List.of(text);
        }
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = start + Math.min(text.length() - start, MAX_MESSAGE_LENGTH);
            if (end < text.length()) {
                int lineEnd = text.lastIndexOf('\n', end - 1) + 1;
                if (lineEnd > start + MAX_MESSAGE_LENGTH / 2) {
                    end = lineEnd;
                } else if (Character.isHighSurrogate(text.charAt(end - 1))) {
                    end--;
                }
            }
            String part = text.substring(start, end);
            if (!part.isBlank()) {
                parts.add(part);
            }
            start = end;
        }
        return List.copyOf(parts);
    }
}
