package com.aram3.infra.process;

import com.aram3.config.Aram3Properties;
import com.aram3.domain.GameInstance;
import com.aram3.repository.InstanceRepository;
import com.aram3.repository.JobRepository;
import com.aram3.service.SettingService;
import com.aram3.service.StartCommandService;
import com.aram3.service.ModLocalService;
import com.aram3.repository.ModRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class ProcessService {
    private static final Logger log = LoggerFactory.getLogger(ProcessService.class);

    private final Aram3Properties properties;
    private final InstanceRepository instanceRepository;
    private final JobRepository jobRepository;
    private final ModRepository modRepository;
    private final ModLocalService modLocalService;
    private final StartCommandService startCommandService;
    private final SettingService settingService;
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final Map<Long, Process> running = new ConcurrentHashMap<>();
    private final Map<Long, String> powershell = new ConcurrentHashMap<>();

    public ProcessService(Aram3Properties properties,
                          InstanceRepository instanceRepository,
                          JobRepository jobRepository,
                          ModRepository modRepository,
                          ModLocalService modLocalService,
                          StartCommandService startCommandService,
                          SettingService settingService) {
        this.properties = properties;
        this.instanceRepository = instanceRepository;
        this.jobRepository = jobRepository;
        this.modRepository = modRepository;
        this.modLocalService = modLocalService;
        this.startCommandService = startCommandService;
        this.settingService = settingService;
    }

    public synchronized String resolvePowerShell() {
        return powershell.computeIfAbsent(1L, k -> detectPowerShell());
    }

    private String detectPowerShell() {
        if (properties.getProcess().isPsPreferPwsh()) {
            String pwsh = findExecutable("pwsh");
            if (pwsh != null) {
                return pwsh;
            }
        }
        String powershell = findExecutable("powershell");
        if (powershell != null) {
            return powershell;
        }
        return properties.getProcess().isPsPreferPwsh() ? "pwsh" : "powershell";
    }

    private String findExecutable(String name) {
        try {
            ProcessBuilder pb = new ProcessBuilder("where", name);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (p.waitFor(5, TimeUnit.SECONDS) && line != null && !line.isBlank()) {
                    return line.trim();
                }
            }
        } catch (Exception e) {
            log.debug("detect {} failed: {}", name, e.getMessage());
        }
        return null;
    }

    public String powershellVersionLine() {
        try {
            ProcessBuilder pb = new ProcessBuilder(resolvePowerShell(), "-NoProfile", "-Command",
                    "$PSVersionTable.PSVersion.ToString()");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                p.waitFor(10, TimeUnit.SECONDS);
                return line == null ? "unknown" : line.trim();
            }
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    public void startInstanceAsync(long jobId, GameInstance inst) {
        pool.submit(() -> {
            Path root = Path.of(inst.rootPath());
            Path exe = resolveGameExe();
            Path logDir = Path.of(settingService.logsDir());
            try {
                Files.createDirectories(logDir);
                Path logFile = logDir.resolve("instance-" + inst.id() + "-" + System.currentTimeMillis() + ".log");
                jobRepository.markRunning(jobId, logFile.toString());
                instanceRepository.updateStatus(inst.id(), "STARTING");

                List<String> cmd = buildStartCommand(exe, inst);
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(root.toFile());
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
                Process process = pb.start();
                running.put(inst.id(), process);
                instanceRepository.updateStatus(inst.id(), "RUNNING");
                jobRepository.markFinished(jobId, "SUCCESS", 100, null);

                int code = process.waitFor();
                running.remove(inst.id());
                instanceRepository.updateStatus(inst.id(), code == 0 ? "STOPPED" : "ERROR");
            } catch (Exception e) {
                log.error("start instance failed", e);
                instanceRepository.updateStatus(inst.id(), "ERROR");
                jobRepository.markFinished(jobId, "FAILED", 0, e.getMessage());
            }
        });
    }

    public void stopInstanceAsync(long jobId, GameInstance inst) {
        pool.submit(() -> {
            try {
                jobRepository.markRunning(jobId, null);
                instanceRepository.updateStatus(inst.id(), "STOPPING");
                Process process = running.get(inst.id());
                if (process != null && process.isAlive()) {
                    process.destroy();
                    if (!process.waitFor(15, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                    running.remove(inst.id());
                } else {
                    // fallback: taskkill by matching root path is environment-specific
                    log.info("no tracked process for instance {}", inst.id());
                }
                instanceRepository.updateStatus(inst.id(), "STOPPED");
                jobRepository.markFinished(jobId, "SUCCESS", 100, null);
            } catch (Exception e) {
                instanceRepository.updateStatus(inst.id(), "ERROR");
                jobRepository.markFinished(jobId, "FAILED", 0, e.getMessage());
            }
        });
    }

    public void downloadWorkshopAsync(long jobId, String workshopId, String mode, Long modId) {
        pool.submit(() -> {
            Path steamcmd = Path.of(settingService.steamcmdPath());
            Path workshopDir = Path.of(settingService.workshopDir());
            try {
                Files.createDirectories(workshopDir);
                Path logFile = Path.of(settingService.logsDir())
                        .resolve("workshop-" + workshopId + "-" + System.currentTimeMillis() + ".log");
                Files.createDirectories(logFile.getParent());
                jobRepository.markRunning(jobId, logFile.toString());

                if ("REDOWNLOAD".equalsIgnoreCase(mode)) {
                    modLocalService.deleteLocal(workshopId, properties.getSteam().getWorkshopAppId());
                    if (modId != null) {
                        modRepository.updateModDownload(modId, "PENDING", null, null, null);
                    }
                }

                List<String> cmd = List.of(
                        steamcmd.toString(),
                        "+login", "anonymous",
                        "+force_install_dir", workshopDir.toString(),
                        "+workshop_download_item", String.valueOf(properties.getSteam().getWorkshopAppId()),
                        workshopId,
                        "+quit"
                );
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
                Process process = pb.start();
                boolean done = process.waitFor(30, TimeUnit.MINUTES);
                if (!done) {
                    process.destroyForcibly();
                    jobRepository.markFinished(jobId, "TIMEOUT", 0, "steamcmd timeout");
                    return;
                }
                int code = process.exitValue();
                Path local = modLocalService.findLocalPath(workshopId, properties.getSteam().getWorkshopAppId());
                if (modId != null) {
                    if (code == 0 && local != null) {
                        modRepository.updateModDownload(modId, "DOWNLOADED", local.toString(),
                                local.getFileName().toString(), dirSize(local));
                    } else if (code == 0) {
                        modRepository.updateModDownload(modId, "DOWNLOADED", null, null, null);
                    } else {
                        modRepository.updateModDownload(modId, "FAILED", null, null, null);
                    }
                }
                jobRepository.markFinished(jobId, code == 0 ? "SUCCESS" : "FAILED", code == 0 ? 100 : 0,
                        code == 0 ? null : "steamcmd exit " + code);
            } catch (Exception e) {
                jobRepository.markFinished(jobId, "FAILED", 0, e.getMessage());
            }
        });
    }

    private Long dirSize(Path dir) {
        try (var walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).mapToLong(p -> {
                try {
                    return Files.size(p);
                } catch (Exception ex) {
                    return 0L;
                }
            }).sum();
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> buildStartCommand(Path exe, GameInstance inst) {
        @SuppressWarnings("unchecked")
        List<String> args = (List<String>) (List<?>) startCommandService.build(inst, exe).get("args");
        return new ArrayList<>(args);
    }

    private Path resolveGameExe() {
        Path dir = Path.of(settingService.gameServerDir());
        Path x64 = dir.resolve("arma3server_x64.exe");
        if (Files.exists(x64)) {
            return x64;
        }
        return dir.resolve("arma3server.exe");
    }

    public boolean isRunning(long instanceId) {
        Process p = running.get(instanceId);
        return p != null && p.isAlive();
    }
}

