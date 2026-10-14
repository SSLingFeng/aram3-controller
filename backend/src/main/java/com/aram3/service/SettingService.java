package com.aram3.service;

import com.aram3.common.BusinessException;
import com.aram3.config.Aram3Properties;
import com.aram3.repository.AuditRepository;
import com.aram3.repository.SettingRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SettingService {
    public static final String KEY_DATA_DIR = "path.dataDir";
    public static final String KEY_GAME_SERVER_DIR = "path.gameServerDir";
    public static final String KEY_STEAMCMD_PATH = "path.steamcmdPath";
    /** Unified mod storage = Workshop download dir */
    public static final String KEY_MODS_DIR = "path.modsDir";
    public static final String KEY_INSTANCES_DIR = "path.instancesDir";
    public static final String KEY_LOGS_DIR = "path.logsDir";

    private final SettingRepository settingRepository;
    private final Aram3Properties properties;
    private final AuditRepository auditRepository;

    public SettingService(SettingRepository settingRepository,
                          Aram3Properties properties,
                          AuditRepository auditRepository) {
        this.settingRepository = settingRepository;
        this.properties = properties;
        this.auditRepository = auditRepository;
    }

    public Map<String, Object> getSettings() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataDir", dataDir());
        result.put("gameServerDir", gameServerDir());
        result.put("steamcmdPath", steamcmdPath());
        result.put("modsDir", modsDir());
        result.put("instancesDir", instancesDir());
        result.put("logsDir", logsDir());
        result.put("mpmissionsDir", mpmissionsDir());
        result.put("steamAppId", properties.getSteam().getAppId());
        result.put("workshopAppId", properties.getSteam().getWorkshopAppId());
        return result;
    }

    public Map<String, Object> updateSettings(Map<String, String> patch) {
        if (patch.containsKey("gameServerDir")) {
            settingRepository.upsert(KEY_GAME_SERVER_DIR, normalize(patch.get("gameServerDir"), "游戏服务器本体目录"), "游戏服务器本体目录");
        }
        if (patch.containsKey("steamcmdPath")) {
            settingRepository.upsert(KEY_STEAMCMD_PATH, normalize(patch.get("steamcmdPath"), "steamcmd.exe 路径"), "steamcmd.exe 路径");
        }
        if (patch.containsKey("modsDir") || patch.containsKey("workshopDir") || patch.containsKey("storageDir")) {
            String v = firstNonNull(patch, "modsDir", "workshopDir", "storageDir");
            settingRepository.upsert(KEY_MODS_DIR, normalize(v, "模组/Workshop 目录"), "模组存储与 Workshop 下载共用目录");
        }
        if (patch.containsKey("instancesDir")) {
            settingRepository.upsert(KEY_INSTANCES_DIR, normalize(patch.get("instancesDir"), "实例配置根目录"), "实例配置根目录");
        }
        if (patch.containsKey("logsDir")) {
            settingRepository.upsert(KEY_LOGS_DIR, normalize(patch.get("logsDir"), "日志目录"), "日志目录");
        }
        if (patch.containsKey("dataDir")) {
            settingRepository.upsert(KEY_DATA_DIR, normalize(patch.get("dataDir"), "数据根目录"), "数据根目录");
        }

        auditRepository.log(null, "settings.update", "system", null, String.valueOf(patch.keySet()), null);
        return getSettings();
    }

    private String firstNonNull(Map<String, String> patch, String... keys) {
        for (String k : keys) {
            if (patch.get(k) != null && !patch.get(k).isBlank()) {
                return patch.get(k);
            }
        }
        return null;
    }

    private String normalize(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(label + "不能为空");
        }
        return value.trim();
    }

    public Path ensureDir(String path, String label) {
        Path p = Path.of(path).toAbsolutePath().normalize();
        try {
            Files.createDirectories(p);
        } catch (Exception e) {
            throw new BusinessException("无法创建目录(" + label + "): " + p + " / " + e.getMessage());
        }
        return p;
    }

    public String gameServerDir() {
        return settingRepository.get(KEY_GAME_SERVER_DIR).orElse(properties.getPaths().getGameServerDir());
    }

    public String steamcmdPath() {
        return settingRepository.get(KEY_STEAMCMD_PATH).orElse(properties.getPaths().getSteamcmdPath());
    }

    /** Workshop download and mod library share this directory. */
    public String modsDir() {
        return settingRepository.get(KEY_MODS_DIR)
                .or(() -> settingRepository.get("path.workshopDir"))
                .or(() -> settingRepository.get("path.storageDir"))
                .orElse(properties.getPaths().getWorkshopDir());
    }

    /** @deprecated use {@link #modsDir()} */
    public String workshopDir() {
        return modsDir();
    }

    /** @deprecated use {@link #modsDir()} */
    public String storageDir() {
        return modsDir();
    }

    /** Maps live in game server root/mpmissions (fixed). */
    public String mpmissionsDir() {
        return Path.of(gameServerDir(), "mpmissions").toString();
    }

    public String instancesDir() {
        return settingRepository.get(KEY_INSTANCES_DIR).orElse(properties.getPaths().getInstancesDir());
    }

    public String logsDir() {
        return settingRepository.get(KEY_LOGS_DIR).orElse(properties.getPaths().getLogsDir());
    }

    public String dataDir() {
        return settingRepository.get(KEY_DATA_DIR).orElse(properties.getPaths().getDataDir());
    }
}
