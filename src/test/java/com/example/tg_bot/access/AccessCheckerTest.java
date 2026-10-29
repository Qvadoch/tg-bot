package com.example.tg_bot.access;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AccessCheckerTest {

    private static final Long ALLOWED_USER = 1L;
    private static final Long OTHER_USER = 3L;

    private final AccessChecker accessChecker = new AccessChecker(Set.of(ALLOWED_USER, 2L));

    @Test
    @DisplayName("id есть в списке -> доступ разрешён")
    void allowsUserFromList() {
        assertTrue(accessChecker.isAllowed(ALLOWED_USER));
    }

    @Test
    @DisplayName("id нет в списке -> доступ запрещён")
    void deniesUserNotInList() {
        assertFalse(accessChecker.isAllowed(OTHER_USER));
    }

    @Test
    @DisplayName("пустой список разрешённых -> доступ запрещён всем")
    void deniesEveryoneWhenListIsEmpty() {
        AccessChecker emptyChecker = new AccessChecker(Set.of());

        assertFalse(emptyChecker.isAllowed(ALLOWED_USER));
    }
}