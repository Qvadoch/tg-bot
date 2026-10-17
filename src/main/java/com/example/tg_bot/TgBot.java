package com.example.tg_bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class TgBot implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(TgBot.class);
    private static final DateTimeFormatter DUE_FORMAT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private final String botToken;
    private final TelegramClient telegramClient;
    private final ClassroomService classroomService;

    public TgBot(@Value("${bot.token}") String botToken, ClassroomService classroomService) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.classroomService = classroomService;
    }


    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Long chatId = update.getMessage().getChatId();
                String command = update.getMessage().getText().trim().split("[ @]")[0];

                switch (command) {
                    case "/start":
                        sendText(chatId, "Привет! Я показываю домашние задания из Google Classroom.\nНапиши /help, чтобы увидеть список команд.");
                        break;
                    case "/help":
                        sendText(chatId, "Команды:\n/start - начать\n/help - список команд\n/homework - показать домашние задания\n/courses - показать курсы");
                        break;
                    case "/homework":
                        sendHomework(chatId);
                        break;
                    case "/courses":
                        sendCourses(chatId);
                        break;
                    default:
                        sendText(chatId, "Такой команды нет. Напиши /help.");
                        break;
                }
            }
        }
    }

    private void sendCourses(Long chatId) {
        try {
            List<String> courses = classroomService.getCourses();
            if (courses.isEmpty()) {
                sendText(chatId, "На данный момент курсов нет.");
                return;
            }
            sendText(chatId, "Твои курсы:\n" + String.join("\n", courses));
        } catch (IOException e) {
            log.error("Не удалось получить курсы из Classroom", e);
            sendText(chatId, "Не удалось получить курсы. Попробуй позже.");
        }
    }

    private void sendHomework(Long chatId) {
        try {
            List<HomeworkItem> homework = classroomService.getHomework();
            if (homework.isEmpty()) {
                sendText(chatId, "Заданий пока нет.");
                return;
            }

            String text = homework.stream()
                    .limit(10)
                    .map(work -> work.courseName() + "\n"
                            + work.title() + "\n"
                            + formatDue(work.due()) + "\n"
                            + work.link())
                    .collect(Collectors.joining("\n\n", "Задания (" + homework.size() + "):\n\n", ""));
            sendText(chatId, text);
        } catch (IOException e) {
            log.error("Не удалось получить задания из Classroom", e);
            sendText(chatId, "Не удалось получить задания. Попробуй позже.");
        }
    }

    private static String formatDue(LocalDateTime due) {
        if (due == null) {
            return "Без срока";
        }
        return "Срок: " + due.format(DUE_FORMAT);
    }

    private void sendText(Long chatId, String text) {
        SendMessage sendMessage = SendMessage.builder().chatId(chatId).text(text).build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение в чат {}", chatId, e);
        }
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }
}
