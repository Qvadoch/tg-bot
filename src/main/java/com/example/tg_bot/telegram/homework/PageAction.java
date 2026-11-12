package com.example.tg_bot.telegram.homework;

// These names are also the callback data stored in Telegram messages.
/**
 * Направление навигации по частям текста и страницам заданий.
 * Имена значений передаются в {@code callbackData}; их изменение влияет
 * на распознавание кнопок уже отправленных сообщений.
 */
enum PageAction {
    NEXT_PAGE, PREVIOUS_PAGE
}
