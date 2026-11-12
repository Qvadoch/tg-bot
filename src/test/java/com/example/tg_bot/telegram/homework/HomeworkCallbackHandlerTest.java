package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.telegram.TelegramSender;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.mockito.Mockito.*;

class HomeworkCallbackHandlerTest {
    private final TelegramSender sender = mock(TelegramSender.class);
    private final HomeworkViewService views = mock(HomeworkViewService.class);
    private final HomeworkCallbackHandler handler = new HomeworkCallbackHandler(sender, views);

    @ParameterizedTest
    @EnumSource(PageAction.class)
    void keyboardPayloadIsRecognizedAndAcknowledgedBeforeNavigation(PageAction action) throws TelegramApiException {
        InlineKeyboardButton button = new HomeworkKeyboard().getInlineKeyboardMarkup().getKeyboard().stream()
                .flatMap(row -> row.stream())
                .filter(candidate -> action.name().equals(candidate.getCallbackData()))
                .findFirst().orElseThrow();

        handler.handle("press", 1L, -100500L, 101, button.getCallbackData());

        InOrder order = inOrder(sender, views);
        order.verify(sender).answerCallbackQuery("press");
        order.verify(views).turnPage(1L, -100500L, 101, action);
        verifyNoMoreInteractions(sender, views);
    }

    @ParameterizedTest
    @EnumSource(PageAction.class)
    void reportsTheOriginalFailureOfEitherNavigationDirection(PageAction action) throws TelegramApiException {
        TelegramApiException failure = new TelegramApiException("Edit rejected");
        doThrow(failure).when(views).turnPage(1L, 10L, 101, action);

        handler.handle("press", 1L, 10L, 101, action.name());

        verify(sender).answerCallbackQuery("press");
        verify(sender).sendFailureNotice(10L,
                "Не удалось перелистнуть страницу. Попробуй снова или отправь /homework.", failure);
        verify(views).turnPage(1L, 10L, 101, action);
        verifyNoMoreInteractions(sender, views);
    }
}
