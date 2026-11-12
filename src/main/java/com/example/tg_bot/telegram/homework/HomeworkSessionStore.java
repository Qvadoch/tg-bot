package com.example.tg_bot.telegram.homework;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Хранит просмотры в памяти одного процесса по сочетанию пользователя и чата.
 * Ограничивает количество записей и время бездействия; при заполнении вытесняет
 * сессию, к которой дольше всего не обращались.
 *
 * <p>Истечение проверяется при чтении и сохранении, фоновой очистки нет.
 * Методы работы с картой синхронизированы. Для последовательности «найти — изменить —
 * сохранить» вызывающий сервис дополнительно использует {@link #lockFor(Conversation)}.
 */
@Component
class HomeworkSessionStore {
    private static final int LOCK_COUNT = 64;

    private final Map<Conversation, CacheEntry> sessions = new LinkedHashMap<>(16, 0.75f, true);
    private final Object[] conversationLocks = new Object[LOCK_COUNT];
    private final Clock clock;
    private final int maximumSize;
    private final Duration idleTimeout;

    /**
     * Создаёт хранилище и постоянный ограниченный набор блокировок разговоров.
     *
     * @param clock источник времени проверки бездействия
     * @param maximumSize максимальное количество сохраняемых сессий
     * @param idleTimeout допустимое время без обращений к записи
     * @throws IllegalArgumentException если размер или время бездействия не положительны
     */
    HomeworkSessionStore(Clock clock, @Value("${bot.homework.sessions.max-size:1000}") int maximumSize,
                         @Value("${bot.homework.sessions.idle-timeout:PT30M}") Duration idleTimeout) {
        if (maximumSize <= 0 || idleTimeout.isNegative() || idleTimeout.isZero()) {
            throw new IllegalArgumentException("Homework session size and idle timeout must be positive");
        }
        this.clock = clock;
        this.maximumSize = maximumSize;
        this.idleTimeout = idleTimeout;
        for (int index = 0; index < conversationLocks.length; index++) {
            conversationLocks[index] = new Object();
        }
    }

    // Locks outlive cached sessions, so expiry or eviction cannot create two concurrent writers for one conversation.
    /**
     * Возвращает стабильную блокировку разговора, независимую от срока жизни его сессии.
     * Разные разговоры могут разделять одну из 64 блокировок и ожидать друг друга.
     *
     * @param conversation сочетание пользователя и чата
     * @return объект для синхронизации операций этого разговора
     */
    Object lockFor(Conversation conversation) {
        return conversationLocks[Math.floorMod(conversation.hashCode(), conversationLocks.length)];
    }

    /**
     * Ищет действующую сессию и обновляет время последнего обращения.
     * При достижении срока бездействия удаляет запись. Успешное чтение также меняет
     * порядок вытеснения, даже если вызывающий код затем пропустит событие.
     *
     * @param conversation сочетание пользователя и чата
     * @return сессия либо {@code null}, если она отсутствует или истекла
     */
    synchronized Session find(Conversation conversation) {
        Instant now = clock.instant();
        CacheEntry entry = sessions.get(conversation);
        if (entry == null) {
            return null;
        }
        if (!entry.lastAccessedAt().plus(idleTimeout).isAfter(now)) {
            sessions.remove(conversation);
            return null;
        }
        sessions.put(conversation, new CacheEntry(entry.session(), now));
        return entry.session();
    }

    /**
     * Удаляет истёкшие записи, сохраняет или заменяет сессию и применяет ограничение размера.
     * Новой записи присваивается текущее время последнего обращения.
     *
     * @param conversation сочетание пользователя и чата
     * @param session результат успешного показа или перехода
     */
    synchronized void save(Conversation conversation, Session session) {
        Instant now = clock.instant();
        sessions.entrySet().removeIf(entry -> !entry.getValue().lastAccessedAt().plus(idleTimeout).isAfter(now));
        sessions.put(conversation, new CacheEntry(session, now));
        while (sessions.size() > maximumSize) {
            Iterator<Conversation> oldest = sessions.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
    }

    /**
     * Ключ просмотра, разделяющий пользователей и чаты.
     *
     * @param userId Telegram ID пользователя
     * @param chatId Telegram ID чата
     */
    record Conversation(long userId, long chatId) {}
    /**
     * Сохранённый просмотр вместе с сообщениями, к которым он относится.
     *
     * @param messages идентификаторы ответа бота и исходной команды
     * @param view выбранная часть подготовленной страницы
     */
    record Session(HomeworkMessagePair messages, HomeworkView view) {}
    /**
     * Внутренняя запись хранилища с отметкой последнего обращения.
     *
     * @param session сохранённый просмотр
     * @param lastAccessedAt момент последнего успешного поиска или сохранения
     */
    private record CacheEntry(Session session, Instant lastAccessedAt) {}
}
