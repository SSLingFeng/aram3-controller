package com.aram3.infra.steam;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interactive SteamCMD session.
 * status: STARTING -> RUNNING -> STOPPED / FAILED
 * User input only while process is alive.
 */
public class SteamCmdSession {

    public enum Status {
        STARTING, RUNNING, STOPPED, FAILED
    }

    private final String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private final List<String> logs = new CopyOnWriteArrayList<>();
    private final AtomicInteger seq = new AtomicInteger();
    private final List<Long> modIds = new CopyOnWriteArrayList<>();
    private final Instant createdAt = Instant.now();

    private volatile Status status = Status.STARTING;
    private volatile String error;
    private volatile Process process;
    private volatile BufferedWriter stdin;
    private Path logFile;
    private volatile String username;
    private volatile String mode = "DIRECT";
    private volatile Integer pid;

    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Long> getModIds() {
        return new ArrayList<>(modIds);
    }

    public String getMode() {
        return mode;
    }

    public String getUsername() {
        return username;
    }

    public Integer getPid() {
        return pid;
    }

    /** Process alive (or not yet created) — page should keep polling. */
    public boolean isActive() {
        return status == Status.STARTING
                || status == Status.RUNNING
                || (process != null && process.isAlive());
    }

    /** Can accept user commands. */
    public boolean canAcceptCommand() {
        return process != null && process.isAlive() && stdin != null
                && (status == Status.RUNNING || status == Status.STARTING);
    }

    public void bind(Process process, BufferedWriter stdin, Path logFile,
                     String username, String mode, List<Long> mods) {
        this.process = process;
        this.stdin = stdin;
        this.logFile = logFile;
        this.username = username;
        this.mode = mode;
        if (mods != null) {
            this.modIds.addAll(mods);
        }
        try {
            var f = Process.class.getMethod("pid").invoke(process);
            this.pid = ((Number) f).intValue();
        } catch (Exception ignored) {
        }
        this.status = Status.RUNNING;
    }

    public void append(String line) {
        if (line == null) {
            return;
        }
        String numbered = seq.incrementAndGet() + "\t" + line;
        logs.add(numbered);
        try {
            if (logFile != null) {
                Files.writeString(logFile, line + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            }
        } catch (Exception ignored) {
        }
    }

    public List<String> logsSince(int sinceSeq) {
        List<String> out = new ArrayList<>();
        for (String line : logs) {
            int idx = line.indexOf('\t');
            if (idx <= 0) {
                continue;
            }
            try {
                int n = Integer.parseInt(line.substring(0, idx));
                if (n > sinceSeq) {
                    out.add(line.substring(idx + 1));
                }
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    public int lastSeq() {
        return seq.get();
    }

    public synchronized boolean sendCommand(String command) {
        if (!canAcceptCommand()) {
            return false;
        }
        try {
            stdin.write(command);
            stdin.newLine();
            stdin.flush();
            append("> " + command);
            return true;
        } catch (Exception e) {
            append("[error] 写入命令失败: " + e.getMessage());
            return false;
        }
    }

    public void stop() {
        Process p = process;
        if (p != null && p.isAlive()) {
            try {
                if (stdin != null) {
                    stdin.write("quit");
                    stdin.newLine();
                    stdin.flush();
                }
            } catch (Exception ignored) {
            }
            p.destroy();
            try {
                if (!p.waitFor(5, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                }
            } catch (Exception ignored) {
                p.destroyForcibly();
            }
        }
        markStopped("用户停止");
    }

    public synchronized void markStopped(String reason) {
        if (status != Status.STOPPED && status != Status.FAILED) {
            status = Status.STOPPED;
            append("[session] SteamCMD 已结束（" + reason + "），指令输入已锁定");
        }
    }

    public synchronized void markFailed(String reason) {
        if (status != Status.STOPPED && status != Status.FAILED) {
            status = Status.FAILED;
            error = reason;
            append("[session] SteamCMD 失败：" + reason + "，指令输入已锁定");
        }
    }

    public Map<String, Object> toView() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("status", status.name());
        m.put("active", isActive());
        m.put("canCommand", canAcceptCommand());
        m.put("pid", pid);
        m.put("error", error);
        m.put("mode", mode);
        m.put("username", username);
        m.put("modIds", new ArrayList<>(modIds));
        m.put("createdAt", createdAt.toString());
        m.put("lastSeq", lastSeq());
        return m;
    }

    public Path getLogFile() {
        return logFile;
    }
}
