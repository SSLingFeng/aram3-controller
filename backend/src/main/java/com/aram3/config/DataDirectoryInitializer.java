package com.aram3.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQLite JDBC requires the parent directory to exist before connecting.
 * Runs before DataSource init (including Spring Boot tests).
 */
public class DataDirectoryInitializer implements EnvironmentPostProcessor {

    private static final Pattern SQLITE_URL_DIR = Pattern.compile(
            "jdbc:sqlite:(?:file:)?(.+?)(?:[?#].*)?$");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ensure("./data");

        String dataDir = environment.getProperty("aram3.paths.data-dir",
                environment.getProperty("ARAM3_DATA_DIR", "./data"));
        ensure(dataDir);

        ensure(environment.getProperty("aram3.paths.logs-dir", "./data/logs"));
        ensure(environment.getProperty("aram3.paths.storage-dir", "./data/storage"));
        ensure(environment.getProperty("aram3.paths.instances-dir", "./data/instances"));
        ensure(environment.getProperty("aram3.paths.workshop-dir", "./data/workshop"));
        ensure(environment.getProperty("aram3.paths.game-server-dir", "./data/game-server"));

        String url = environment.getProperty("spring.datasource.url");
        if (url != null && url.startsWith("jdbc:sqlite:")) {
            Matcher m = SQLITE_URL_DIR.matcher(url);
            if (m.find()) {
                String filePart = m.group(1);
                if (!filePart.isBlank()) {
                    Path p = Path.of(filePart);
                    Path parent = p.toAbsolutePath().normalize().getParent();
                    if (parent != null) {
                        ensure(parent.toString());
                    }
                }
            }
            // if still using relative ./data style after placeholder resolution attempt
            if (url.contains("data")) {
                ensure("./data");
            }
        }
    }

    private void ensure(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        try {
            Path p = Path.of(path).toAbsolutePath().normalize();
            Files.createDirectories(p);
        } catch (Exception ignored) {
            // best-effort
        }
    }
}
