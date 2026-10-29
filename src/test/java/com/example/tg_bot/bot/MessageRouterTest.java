package com.example.tg_bot.bot;

import com.example.tg_bot.handler.CommandHandler;
import com.example.tg_bot.handler.DefaultHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageRouterTest {
    private static final Long CHAT_ID = 1L;

    @Mock
    DefaultHandler defaultHandler;
    @Mock
    CommandHandler startHandler;
    @Mock
    CommandHandler helpHandler;
    @Mock
    CommandHandler coursesHandler;
    @Mock
    CommandHandler homeworkHandler;

    MessageRouter messageRouter;

    @BeforeEach
    void setUp() {
        when(startHandler.command()).thenReturn("/start");
        when(helpHandler.command()).thenReturn("/help");
        when(coursesHandler.command()).thenReturn("/courses");
        when(homeworkHandler.command()).thenReturn("/homework");

        messageRouter = new MessageRouter(
                List.of(startHandler, helpHandler, coursesHandler, homeworkHandler), defaultHandler);
    }

    @Test
    @DisplayName("/start -> вызывается только StartHandler")
    void routesStartToStartHandler() {
        messageRouter.route(CHAT_ID, "/start");

        verify(startHandler).handle(CHAT_ID);
        verify(helpHandler, never()).handle(CHAT_ID);
        verify(coursesHandler, never()).handle(CHAT_ID);
        verify(homeworkHandler, never()).handle(CHAT_ID);
        verifyNoInteractions(defaultHandler);
    }

    @Test
    @DisplayName("/help -> вызывается только HelpHandler")
    void routesHelpToHelpHandler() {
        messageRouter.route(CHAT_ID, "/help");

        verify(helpHandler).handle(CHAT_ID);
        verify(startHandler, never()).handle(CHAT_ID);
        verifyNoInteractions(defaultHandler);
    }

    @Test
    @DisplayName("/courses -> вызывается только CoursesHandler")
    void routesCoursesToCoursesHandler() {
        messageRouter.route(CHAT_ID, "/courses");

        verify(coursesHandler).handle(CHAT_ID);
        verify(startHandler, never()).handle(CHAT_ID);
        verifyNoInteractions(defaultHandler);
    }

    @Test
    @DisplayName("/homework -> вызывается только HomeworkHandler")
    void routesHomeworkToHomeworkHandler() {
        messageRouter.route(CHAT_ID, "/homework");

        verify(homeworkHandler).handle(CHAT_ID);
        verify(startHandler, never()).handle(CHAT_ID);
        verifyNoInteractions(defaultHandler);
    }

    @Test
    @DisplayName("неизвестная команда -> вызывается DefaultHandler, остальные не трогаются")
    void routesUnknownCommandToDefaultHandler() {
        messageRouter.route(CHAT_ID, "/unknown");

        verify(defaultHandler).handle(CHAT_ID);
        verify(startHandler, never()).handle(CHAT_ID);
        verify(helpHandler, never()).handle(CHAT_ID);
        verify(coursesHandler, never()).handle(CHAT_ID);
        verify(homeworkHandler, never()).handle(CHAT_ID);
    }

    @Test
    @DisplayName("две одинаковые команды -> IllegalStateException при создании роутера")
    void throwsWhenTwoHandlersHaveSameCommand() {
        when(helpHandler.command()).thenReturn("/start");

        assertThrows(IllegalStateException.class,
                () -> new MessageRouter(List.of(startHandler, helpHandler), defaultHandler));
    }
}