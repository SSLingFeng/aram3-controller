package com.aram3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class Aram3Application {
    public static void main(String[] args) {
        // SQLite needs the parent folder to exist before JDBC connects
        String dataDir = System.getenv("ARAM3_DATA_DIR");
        if (dataDir == null || dataDir.isBlank()) {
            dataDir = System.getProperty("aram3.paths.data-dir", "./data");
        }
        try {
            Files.createDirectories(Path.of(dataDir).toAbsolutePath().normalize());
        } catch (Exception e) {
            System.err.println("Failed to create data dir: " + dataDir + " -> " + e.getMessage());
        }
        SpringApplication.run(Aram3Application.class, args);
    }
}
