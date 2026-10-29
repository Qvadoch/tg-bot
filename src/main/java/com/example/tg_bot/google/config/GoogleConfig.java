package com.example.tg_bot.google.config;

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
import java.io.FileReader;
import java.util.Set;

@Configuration
public class GoogleConfig {
    private final String googleTokensDir;
    private final String googleClientFile;
    private final Set<String> scopes = Set.of(
            "https://www.googleapis.com/auth/classroom.courses.readonly",
            "https://www.googleapis.com/auth/classroom.course-work.readonly",
            "https://www.googleapis.com/auth/classroom.coursework.me.readonly");
    private NetHttpTransport httpTransport;
    private GsonFactory jsonFactory;

    public GoogleConfig(@Value("${google.tokens.dir}") String googleTokensDir, @Value("${google.client-file}") String googleClientFile) {
        this.googleTokensDir = googleTokensDir;
        this.googleClientFile = googleClientFile;
    }

    @Bean
    public Classroom classroom() throws Exception {
        httpTransport = new NetHttpTransport.Builder().build();
        jsonFactory = GsonFactory.getDefaultInstance();
        Credential credential = getCredentials();
        return new Classroom.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("tg_bot")
                .build();
    }

    private Credential getCredentials() throws Exception {
        GoogleClientSecrets secrets;
        try (FileReader reader = new FileReader(googleClientFile)) {
            secrets = GoogleClientSecrets.load(jsonFactory, reader);
        }
        FileDataStoreFactory dataStore = new FileDataStoreFactory(new File(googleTokensDir));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, secrets, scopes)
                .setDataStoreFactory(dataStore)
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
        LocalServerReceiver localServerReceiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, localServerReceiver).authorize("user");
    }
}
