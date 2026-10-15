package com.example.tg_bot;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ClassroomService {
    private final String googleTokensDir;
    private final String googleClientFile;
    private final Set<String> scopes = Set.of("https://www.googleapis.com/auth/classroom.courses.readonly", "https://www.googleapis.com/auth/classroom.course-work.readonly", "https://www.googleapis.com/auth/classroom.coursework.me.readonly");
    private NetHttpTransport httpTransport;
    private GsonFactory jsonFactory;
    private Classroom classroom;

    public ClassroomService(@Value("${google.tokens.dir}") String googleTokensDir, @Value("${google.client-file}") String googleClientFile) {
        this.googleTokensDir = googleTokensDir;
        this.googleClientFile = googleClientFile;
    }

    @PostConstruct
    private void init() throws Exception {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        jsonFactory = GsonFactory.getDefaultInstance();
        Credential credential = getCredentials();
        classroom = new Classroom.Builder(httpTransport, jsonFactory, credential)
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

    public Classroom getClassroom() {
        return classroom;
    }

    public List<String> getCourses() throws IOException {
        List<Course> courses = classroom.courses().list()
                .setCourseStates(List.of("ACTIVE"))
                .execute()
                .getCourses();
        if (courses == null) {
            return List.of();
        }
        return courses.stream()
                .map(Course::getName)
                .toList();
    }

    public List<CourseWork> getHomework() throws IOException {
        List<Course> courses = classroom.courses().list()
                .setCourseStates(List.of("ACTIVE"))
                .execute()
                .getCourses();
        if (courses == null) {
            return List.of();
        }

        List<CourseWork> result = new ArrayList<>();
        for (Course course : courses) {
            List<CourseWork> works = classroom.courses().courseWork()
                    .list(course.getId())
                    .execute()
                    .getCourseWork();
            if (works != null) {
                result.addAll(works);
            }
        }
        return result;
    }
}
