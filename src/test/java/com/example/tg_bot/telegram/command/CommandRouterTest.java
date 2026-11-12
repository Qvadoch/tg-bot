package com.example.tg_bot.telegram.command;

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
class CommandRouterTest {
    private static final Long USER_ID = 10L;
    private static final Long CHAT_ID = 1L;
    private static final Integer COMMAND_MESSAGE_ID = 100;

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

    CommandRouter commandRouter;

    @BeforeEach
    void setUp() {
        when(startHandler.command()).thenReturn("/start");
        when(helpHandler.command()).thenReturn("/help");
        when(coursesHandler.command()).thenReturn("/courses");
        when(homeworkHandler.command()).thenReturn("/homework");

        commandRouter = new CommandRouter(
                List.of(startHandler, helpHandler, coursesHandler, homeworkHandler), defaultHandler);
    }

    @Test
    @DisplayName("/start -> вызывается только StartHandler")
    void routesStartToStartHandler() {
        commandRouter.route(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID, "/start");

        verify(startHandler).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
        verify(helpHandler, never()).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
        verify(coursesHandler, never()).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
        verify(homeworkHandler, never()).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
        verifyNoInteractions(defaultHandler);
    }

    @Test
    @DisplayName("/help -> вызывается только HelpHandler")
    void routesHelpToHelpHandler() {
        commandRouter.route(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID, "/help");

        verify(helpHandler).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
        verify(startHandler, never()).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
        verifyNoInteractions(defaultHandler);
    }

    @Test
    @DisplayName("/courses -> вызывается только CoursesHandler")
    void routesCoursesToCoursesHandler() {
        commandRouter.route(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID, "/courses");

        verify(coursesHandler).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
        verify(startHandler, never()).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
        verifyNoInteractions(defaultHandler);
    }

    @Test
    @DisplayName("/homework -> вызывается только HomeworkHandler")
    void routesHomeworkToHomeworkHandler() {
        commandRouter.route(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID, "/homework");

        verify(homeworkHandler).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
        verify(startHandler, never()).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
        verifyNoInteractions(defaultHandler);
    }

    @Test
    @DisplayName("неизвестная команда -> вызывается DefaultHandler, остальные не трогаются")
    void routesUnknownCommandToDefaultHandler() {
        commandRouter.route(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID, "/unknown");

        verify(defaultHandler).handle(CHAT_ID);
        verify(startHandler, never()).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
        verify(helpHandler, never()).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
        verify(coursesHandler, never()).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
        verify(homeworkHandler, never()).handle(USER_ID, CHAT_ID, COMMAND_MESSAGE_ID);
    }

    @Test
    @DisplayName("две одинаковые команды -> IllegalStateException при создании роутера")
    void throwsWhenTwoHandlersHaveSameCommand() {
        when(helpHandler.command()).thenReturn("/start");

        assertThrows(IllegalStateException.class,
                () -> new CommandRouter(List.of(startHandler, helpHandler), defaultHandler));
    }
}
