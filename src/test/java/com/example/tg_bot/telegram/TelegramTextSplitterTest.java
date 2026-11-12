package com.example.tg_bot.telegram;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Random;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TelegramTextSplitterTest {
    private final TelegramTextSplitter splitter = new TelegramTextSplitter();

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 4095, 4096, 4097, 8192, 8193})
    void preservesTextAtMessageLengthBoundaries(int length) {
        String text = "x".repeat(length);
        List<String> parts = splitter.split(text);
        assertEquals(text, String.join("", parts));
        assertEquals(Math.max(1, (length + 4095) / 4096), parts.size());
        assertTrue(parts.stream().allMatch(part -> part.length() <= 4096));
    }

    @Test
    void prefersALineBoundaryWithoutDiscardingTheNewline() {
        String firstLine = "a".repeat(3000) + "\n";
        String secondLine = "b".repeat(2000);
        assertEquals(List.of(firstLine, secondLine), splitter.split(firstLine + secondLine));
    }

    @Test
    void doesNotCreateATinyPartToUseAnEarlyLineBreak() {
        String text = "a\n" + "b".repeat(4998);
        List<String> parts = splitter.split(text);
        assertEquals(4096, parts.getFirst().length());
        assertEquals(text, String.join("", parts));
    }

    @ParameterizedTest
    @ValueSource(ints = {4094, 4095, 4096, 8191})
    void keepsSurrogatePairsTogetherAtAndAroundBoundaries(int prefixLength) {
        String text = "a".repeat(prefixLength) + "📗" + "b".repeat(5000);
        List<String> parts = splitter.split(text);
        assertEquals(text, String.join("", parts));
        for (String part : parts) {
            assertTrue(part.length() <= 4096);
            assertFalse(Character.isLowSurrogate(part.charAt(0)));
            assertFalse(Character.isHighSurrogate(part.charAt(part.length() - 1)));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\n\t"})
    void preservesShortBlankInput(String text) {
        assertEquals(List.of(text), splitter.split(text));
    }

    @Test
    void omitsEntirelyBlankPartsOfLongInput() {
        // This is the existing contract: long blank parts are deliberately filtered out.
        assertTrue(splitter.split(" ".repeat(5000)).isEmpty());
        assertEquals(List.of("hello"), splitter.split(" ".repeat(4096) + "hello"));
    }

    @Test
    void returnsAnUnmodifiableResult() {
        List<String> parts = splitter.split("x".repeat(5000));
        assertThrows(UnsupportedOperationException.class, () -> parts.add("extra"));
        assertThrows(UnsupportedOperationException.class, () -> parts.set(0, "replacement"));
    }

    @Test
    void usesTheLastSuitableLineBreak() {
        String completeLines = "a".repeat(2500) + "\n" + "б".repeat(1000) + "\n";
        String remainingLine = "c".repeat(2000);

        assertEquals(List.of(completeLines, remainingLine), splitter.split(completeLines + remainingLine));
    }

    @Test
    void ignoresLineBreaksThatBelongToAnAlreadyEmittedPart() {
        String firstLine = "a".repeat(3000) + "\n";
        String text = firstLine + "б".repeat(9000);
        List<String> parts = splitter.split(text);

        assertEquals(firstLine, parts.getFirst());
        assertEquals(text, String.join("", parts));
        assertEquals(4, parts.size());
        assertTrue(parts.stream().allMatch(part -> part.length() <= 4096));
    }

    @Test
    void preservesMixedUnicodeTextAcrossManyMessageBoundaries() {
        Random random = new Random(20261112L);
        List<String> alphabet = List.of("Я", "a", "漢", "📗", "🙂", "e\u0301", " ", "\n");
        for (int sample = 0; sample < 32; sample++) {
            StringBuilder input = new StringBuilder();
            for (int block = 0; block < 300; block++) {
                input.append("Задание ");
                for (int index = 0; index < 40; index++) {
                    input.append(alphabet.get(random.nextInt(alphabet.size())));
                }
            }
            String text = input.append("Конец").toString();
            List<String> parts = splitter.split(text);

            assertTrue(parts.size() > 1, "Sample " + sample);
            assertEquals(text, String.join("", parts), "Sample " + sample);
            for (String part : parts) {
                assertFalse(part.isBlank());
                assertTrue(part.length() <= 4096);
                assertTrue(StandardCharsets.UTF_8.newEncoder().canEncode(part), "A Unicode character was split");
            }
        }
    }
}
