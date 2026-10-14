package com.aram3;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest
class Aram3ApplicationTests {

    @BeforeAll
    static void prepareDataDir() throws IOException {
        // tests use ./data/aram3.db by default — parent must exist before SQLite opens
        Files.createDirectories(Path.of("./data").toAbsolutePath().normalize());
    }

    @Test
    void contextLoads() {
    }
}
