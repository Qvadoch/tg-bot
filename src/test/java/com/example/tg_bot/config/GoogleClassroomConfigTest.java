package com.example.tg_bot.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GoogleClassroomConfigTest {
    @TempDir
    Path directory;

    @Test
    void failsBeforeAuthorizationWhenTheClientFileIsMissing() {
        Path clientFile = directory.resolve("missing-client.json");
        Path tokensDirectory = directory.resolve("tokens");
        GoogleClassroomConfig config = new GoogleClassroomConfig(tokensDirectory.toString(), clientFile.toString());

        NoSuchFileException error = assertThrows(NoSuchFileException.class, config::classroom);

        assertEquals(clientFile.toString(), error.getFile());
        assertFalse(Files.exists(tokensDirectory));
    }
}
