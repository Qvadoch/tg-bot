package com.example.tg_bot;

import java.time.LocalDateTime;

public record HomeworkItem(String courseName, String title, String link, LocalDateTime due) {
}
