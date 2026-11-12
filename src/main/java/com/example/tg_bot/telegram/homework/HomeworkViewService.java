package com.example.tg_bot.telegram.homework;

import com.example.tg_bot.homework.model.HomeworkItem;
import com.example.tg_bot.homework.pagination.HomeworkPage;
import com.example.tg_bot.telegram.TelegramSender;
import com.example.tg_bot.telegram.homework.HomeworkSessionStore.Conversation;
import com.example.tg_bot.telegram.homework.HomeworkSessionStore.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Objects;

/**
 * Управляет показом заданий и переходами по частям текста и страницам.
 * Операции одного разговора выполняются последовательно под блокировкой хранилища.
 * Новый просмотр сохраняется после подтверждённой отправки или редактирования.
 *
 * <p>При неопределённом сетевом результате локальный просмотр может временно
 * отличаться от содержимого Telegram. Ошибки основной операции передаются обработчику;
 * ошибки удаления прежних сообщений только записываются в журнал.
 */
@Component
class HomeworkViewService {
    private static final Logger log = LoggerFactory.getLogger(HomeworkViewService.class);

    private final HomeworkSessionStore sessions;
    private final TelegramSender sender;
    private final HomeworkPageRenderer renderer;

    HomeworkViewService(HomeworkSessionStore sessions, TelegramSender sender, HomeworkPageRenderer renderer) {
        this.sessions = sessions;
        this.sender = sender;
        this.renderer = renderer;
    }

    /**
     * Показывает первую часть нового снимка заданий и сохраняет её вместе с ID сообщений.
     * Если сохранена команда с таким же или большим ID, пропускает показ.
     * После сохранения нового просмотра пытается удалить прежний ответ и команду.
     *
     * <p>При ошибке отправки сохранение и удаление не выполняются. Проверка повторов
     * действует только пока прежняя сессия доступна в хранилище.
     *
     * @param userId ID пользователя
     * @param chatId ID чата
     * @param commandMessageId ID команды {@code /homework}
     * @param homework загруженный список заданий; может быть пустым
     * @throws TelegramApiException если отправка основного ответа не подтверждена
     */
    void show(long userId, long chatId, int commandMessageId, List<HomeworkItem> homework) throws TelegramApiException {
        Conversation conversation = new Conversation(userId, chatId);
        synchronized (sessions.lockFor(conversation)) {
            Session previous = sessions.find(conversation);
            if (previous != null && previous.messages().commandMessageId() >= commandMessageId) {
                return;
            }
            RenderedHomeworkPage renderedPage = renderer.render(HomeworkPage.first(homework));
            HomeworkView view = new HomeworkView(renderedPage, 0);
            int botMessageId = sender.sendMessage(chatId, view.text(), view.keyboard());
            HomeworkMessagePair messages = new HomeworkMessagePair(chatId, botMessageId, commandMessageId);
            sessions.save(conversation, new Session(messages, view));

            if (previous != null) {
                deleteOldMessage(chatId, previous.messages().botMessageId());
                deleteOldMessage(chatId, previous.messages().commandMessageId());
            }
        }
    }

    /**
     * Переключает активный ответ на соседнюю часть текста или страницу заданий.
     * Пропускает нажатия старого сообщения и переходы за границы снимка.
     * При отсутствии сессии предлагает снова вызвать {@code /homework}.
     *
     * <p>Если подготовленный текст и клавиатура совпадают с текущими, сохраняет новое
     * положение без запроса редактирования. Иначе сохраняет его после успешного запроса.
     *
     * @param userId ID пользователя
     * @param chatId ID чата
     * @param messageId ID сообщения, на котором нажата кнопка
     * @param action направление перехода
     * @throws TelegramApiException если редактирование сообщения не подтверждено
     */
    void turnPage(long userId, long chatId, int messageId, PageAction action) throws TelegramApiException {
        Conversation conversation = new Conversation(userId, chatId);
        synchronized (sessions.lockFor(conversation)) {
            Session session = sessions.find(conversation);
            if (session == null) {
                sender.sendText(chatId, "Чтобы загрузить задания, напиши /homework");
                return;
            }
            if (session.messages().botMessageId() != messageId) {
                return;
            }
            HomeworkView previous = session.view();
            HomeworkView target = prepareTurn(previous, action);
            if (target == previous) {
                return;
            }
            if (!target.text().equals(previous.text()) || !Objects.equals(target.keyboard(), previous.keyboard())) {
                sender.editMessage(chatId, messageId, target.text(), target.keyboard());
            }
            sessions.save(conversation, new Session(session.messages(), target));
        }
    }

    /**
     * Готовит переход без изменения сохранённой сессии и без сетевых вызовов.
     * Сначала меняет часть текста. За её границами переходит к соседней десятке заданий:
     * вперёд открывает первую часть, назад — последнюю.
     *
     * @param current текущий просмотр
     * @param action направление перехода
     * @return подготовленный просмотр либо {@code current}, если переход невозможен
     */
    private HomeworkView prepareTurn(HomeworkView current, PageAction action) {
        boolean forward = action == PageAction.NEXT_PAGE;
        int targetPartIndex = current.messagePartIndex() + (forward ? 1 : -1);
        RenderedHomeworkPage renderedPage = current.renderedPage();
        if (targetPartIndex >= 0 && targetPartIndex < renderedPage.messageParts().size()) {
            return new HomeworkView(renderedPage, targetPartIndex);
        }

        HomeworkPage page = renderedPage.page();
        HomeworkPage targetPage = forward ? page.next() : page.previous();
        if (targetPage == page) {
            return current;
        }
        RenderedHomeworkPage target = renderer.render(targetPage);
        int partIndex = forward ? 0 : target.messageParts().size() - 1;
        return new HomeworkView(target, partIndex);
    }

    /**
     * Пытается удалить одно прежнее сообщение, сохраняя возможность удалить второе при ошибке.
     *
     * @param chatId ID чата
     * @param messageId ID удаляемого сообщения
     */
    private void deleteOldMessage(long chatId, int messageId) {
        try {
            sender.deleteMessage(chatId, messageId);
        } catch (TelegramApiException exception) {
            log.warn("Failed to delete previous homework message {} in Telegram chat {}", messageId, chatId, exception);
        }
    }
}
