package com.example.tg_bot.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.classroom.Classroom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Настраивает общий Google Classroom-клиент для всех разрешённых пользователей бота.
 * Параметры OAuth и сохранённые разрешения читаются из внешних файлов.
 *
 * <p>Первичное подключение использует локальный OAuth-вход через порт 8888.
 * Выбор отдельного Google-аккаунта для каждого Telegram-пользователя здесь не реализован.
 */
@Configuration
public class GoogleClassroomConfig {
    private final String googleTokensDir;
    private final String googleClientFile;
    private static final Set<String> SCOPES = Set.of(
            "https://www.googleapis.com/auth/classroom.courses.readonly",
            "https://www.googleapis.com/auth/classroom.coursework.me.readonly");

    public GoogleClassroomConfig(@Value("${google.tokens.dir}") String googleTokensDir, @Value("${google.client-file}") String googleClientFile) {
        this.googleTokensDir = googleTokensDir;
        this.googleClientFile = googleClientFile;
    }

    /**
     * Создаёт клиент Classroom с HTTP-транспортом, JSON-фабрикой и общими разрешениями.
     *
     * @return авторизованный клиент Google Classroom
     * @throws IOException если не удалось прочитать конфигурацию или получить разрешения
     */
    @Bean
    public Classroom classroom() throws IOException {
        NetHttpTransport httpTransport = new NetHttpTransport.Builder().build();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Credential credential = getCredentials(httpTransport, jsonFactory);
        return new Classroom.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("tg_bot")
                .build();
    }

    /**
     * Загружает параметры OAuth и получает либо восстанавливает разрешения для ключа {@code user}.
     * Файл параметров закрывается после чтения; сохранённые разрешения находятся в {@code googleTokensDir}.
     *
     * @param httpTransport транспорт, общий с клиентом Classroom
     * @param jsonFactory фабрика обработки JSON
     * @return разрешения общего Google-аккаунта
     * @throws IOException при ошибке чтения, хранения или обмена данными OAuth
     */
    private Credential getCredentials(NetHttpTransport httpTransport, GsonFactory jsonFactory) throws IOException {
        GoogleClientSecrets secrets;
        try (Reader reader = Files.newBufferedReader(Path.of(googleClientFile))) {
            secrets = GoogleClientSecrets.load(jsonFactory, reader);
        }
        FileDataStoreFactory dataStore = new FileDataStoreFactory(new File(googleTokensDir));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, secrets, SCOPES)
                .setDataStoreFactory(dataStore)
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
        LocalServerReceiver localServerReceiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, localServerReceiver).authorize("user");
    }
}
