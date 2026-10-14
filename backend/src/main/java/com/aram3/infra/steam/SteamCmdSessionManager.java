package com.aram3.infra.steam;

import com.aram3.common.BusinessException;
import com.aram3.config.Aram3Properties;
import com.aram3.domain.ModCatalog;
import com.aram3.repository.AuditRepository;
import com.aram3.repository.ModRepository;
import com.aram3.service.ModLocalService;
import com.aram3.service.SettingService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class SteamCmdSessionManager {
    private static final Logger log = LoggerFactory.getLogger(SteamCmdSessionManager.class);

    private final Map<String, SteamCmdSession> sessions = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final SettingService settingService;
    private final Aram3Properties properties;
    private final ModRepository modRepository;
    private final ModLocalService modLocalService;
    private final AuditRepository auditRepository;

    public SteamCmdSessionManager(SettingService settingService,
                                  Aram3Properties properties,
                                  ModRepository modRepository,
                                  ModLocalService modLocalService,
                                  AuditRepository auditRepository) {
        this.settingService = settingService;
        this.properties = properties;
        this.modRepository = modRepository;
        this.modLocalService = modLocalService;
        this.auditRepository = auditRepository;
    }

    @PreDestroy
    public void shutdown() {
        sessions.values().forEach(SteamCmdSession::stop);
        pool.shutdownNow();
    }

    public SteamCmdSession get(String sessionId) {
        SteamCmdSession s = sessions.get(sessionId);
        if (s == null) {
            throw new BusinessException("SteamCMD 会话不存在或已结束");
        }
        return s;
    }

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> list = new ArrayList<>();
        sessions.values().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .forEach(s -> list.add(s.toView()));
        return list;
    }

    public SteamCmdSession start(String username, String password, String guardCode,
                                 String mode, boolean all, List<Long> modIds, Long userId) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("Steam 账号和密码必填");
        }
        if (guardCode != null && guardCode.isBlank()) {
            guardCode = null;
        }
        if (guardCode != null && !guardCode.matches("[A-Za-z0-9]{4,12}")) {
            throw new BusinessException("令牌应为 4-12 位数字或字母（可留空）");
        }

        List<ModCatalog> targets = new ArrayList<>();
        if (all) {
            targets.addAll(modRepository.findAllMods());
        } else if (modIds != null && !modIds.isEmpty()) {
            for (Long id : modIds) {
                ModCatalog m = modRepository.findModById(id)
                        .orElseThrow(() -> new BusinessException("模组不存在: " + id));
                targets.add(m);
            }
        }
        if (targets.isEmpty()) {
            throw new BusinessException("请选择要更新的模组");
        }

        final String user = username.trim();
        final String pass = password;
        final String guard = guardCode;
        final String runMode = "REDOWNLOAD".equalsIgnoreCase(mode) ? "REDOWNLOAD" : "DIRECT";
        final List<ModCatalog> targetMods = List.copyOf(targets);

        final SteamCmdSession session = new SteamCmdSession();
        sessions.put(session.getId(), session);

        pool.submit(() -> runSession(session, user, pass, guard, runMode, targetMods));

        auditRepository.log(userId, "steamcmd.session.start", "steamcmd", null,
                "mods=" + targetMods.size() + ",mode=" + runMode, null);
        return session;
    }

    private void runSession(SteamCmdSession session, String user, String pass,
                            String guard, String runMode, List<ModCatalog> targetMods) {
        Path workshopDir = null;
        try {
            Path steamcmd = Path.of(settingService.steamcmdPath());
            workshopDir = Path.of(settingService.workshopDir());
            Files.createDirectories(workshopDir);
            Path logFile = Path.of(settingService.logsDir())
                    .resolve("steamcmd-" + session.getId() + ".log");
            Files.createDirectories(logFile.getParent());

            String appId = String.valueOf(properties.getSteam().getWorkshopAppId());
            List<String> cmd = new ArrayList<>();
            cmd.add(steamcmd.toString());
            cmd.add("+force_install_dir");
            cmd.add(workshopDir.toString());
            cmd.add("+login");
            cmd.add(user);
            cmd.add(pass);
            if (guard != null && !guard.isBlank()) {
                cmd.add(guard);
            }
            for (ModCatalog m : targetMods) {
                if ("REDOWNLOAD".equals(runMode)) {
                    try {
                        modLocalService.deleteLocal(m.workshopId(), properties.getSteam().getWorkshopAppId());
                    } catch (Exception e) {
                        session.append("[warn] 删除本地模组失败: " + m.workshopId() + " " + e.getMessage());
                    }
                }
                cmd.add("+workshop_download_item");
                cmd.add(appId);
                cmd.add(m.workshopId());
            }
            cmd.add("+quit");

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.directory(workshopDir.toFile());
            Process process = pb.start();

            BufferedWriter stdin = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            session.bind(process, stdin, logFile, user, runMode,
                    targetMods.stream().map(ModCatalog::id).toList());

            session.append("[session] 准备启动 SteamCMD...");
            session.append("[session] 程序: " + steamcmd);
            session.append("[session] 工作目录: " + workshopDir);
            session.append("[session] 登录账号: " + user + (guard == null ? "" : "（含令牌）"));
            session.append("[session] 模组数: " + targetMods.size() + "，模式: " + runMode);
            for (ModCatalog m : targetMods) {
                session.append("[session] 待下载: " + m.name() + " workshopId=" + m.workshopId());
            }
            log.info("[steamcmd:{}] start user={} mods={} mode={} steamcmd={}",
                    session.getId(), user, targetMods.size(), runMode, steamcmd);
            session.append("[session] 进程创建中...");
            session.append("[session] SteamCMD 启动，账号=" + user
                    + (guard == null ? "" : "，含令牌")
                    + "，模组数=" + targetMods.size()
                    + "，模式=" + runMode);

            Thread reader = new Thread(() -> readOutput(session, process), "steamcmd-out-" + session.getId());
            reader.setDaemon(true);
            reader.start();

            session.append("[session] 等待 SteamCMD 结束（最长 60 分钟）...");
            boolean finished = process.waitFor(60, TimeUnit.MINUTES);
            int code = finished ? process.exitValue() : -1;
            if (code == -1 && process.isAlive()) {
                process.destroyForcibly();
                session.markStopped("timeout");
            } else {
                session.markStopped("exit=" + code);
            }
            afterExit(session, targetMods, code);
        } catch (Exception e) {
            log.error("steamcmd session failed", e);
            session.markFailed(e.getMessage());
            afterExit(session, targetMods, -1);
        }
    }

    private void readOutput(SteamCmdSession session, Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                session.append(line);
                log.info("[steamcmd:{}] {}", session.getId(), line);
            }
        } catch (Exception e) {
            session.append("[error] 读取输出失败: " + e.getMessage());
            log.warn("[steamcmd:{}] output reader failed: {}", session.getId(), e.toString());
        }
    }

    private void afterExit(SteamCmdSession session, List<ModCatalog> targets, int code) {
        int ok = 0;
        int missing = 0;
        for (ModCatalog m : targets) {
            Map<String, Object> local = modLocalService.describeById(m.id(), m.workshopId(), null);
            boolean exists = "DOWNLOADED".equals(local.get("localStatus"));
            String path = String.valueOf(local.get("localPath"));
            if (exists) {
                ok++;
                modRepository.updateModDownload(m.id(), "DOWNLOADED",
                        path.isBlank() ? null : path, null, null);
                session.append("[check] 模组 " + m.name() + " (" + m.workshopId() + ") 本地存在: " + path);
            } else {
                missing++;
                modRepository.updateModDownload(m.id(), code == 0 ? "MISSING" : "FAILED", null, null, null);
                session.append("[check] 模组 " + m.name() + " (" + m.workshopId() + ") 本地不存在");
            }
        }
        session.append("[session] 结束检查完成：存在 " + ok + "，不存在 " + missing + "；输入已锁定");
    }

    public Map<String, Object> sendCommand(String sessionId, String command, Long userId) {
        SteamCmdSession s = get(sessionId);
        if (command == null || command.isBlank()) {
            throw new BusinessException("指令不能为空");
        }
        String cmd = command.replace("\r", " ").replace("\n", " ").trim();
        if (cmd.length() > 500) {
            throw new BusinessException("指令过长");
        }
        if (!s.canAcceptCommand()) {
            throw new BusinessException("SteamCMD 已停止或未就绪，无法再输入指令（已锁定）");
        }
        boolean ok = s.sendCommand(cmd);
        if (!ok) {
            throw new BusinessException("发送失败，SteamCMD 可能已退出");
        }
        auditRepository.log(userId, "steamcmd.session.command", "steamcmd", null,
                "session=" + sessionId, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("session", s.toView());
        return result;
    }

    public Map<String, Object> stop(String sessionId, Long userId) {
        SteamCmdSession s = get(sessionId);
        s.stop();
        afterExit(s, s.getModIds().stream()
                .map(id -> modRepository.findModById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList(), -2);
        auditRepository.log(userId, "steamcmd.session.stop", "steamcmd", null, sessionId, null);
        return s.toView();
    }

    public Map<String, Object> logs(String sessionId, int sinceSeq) {
        SteamCmdSession s = get(sessionId);
        Map<String, Object> m = new LinkedHashMap<>(s.toView());
        m.put("logs", s.logsSince(sinceSeq));
        m.put("nextSeq", s.lastSeq());
        return m;
    }
}
