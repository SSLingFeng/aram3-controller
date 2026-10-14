package com.aram3.service;

import com.aram3.domain.GameInstance;
import com.aram3.domain.InstanceMod;
import com.aram3.domain.ModCatalog;
import com.aram3.repository.ModRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds Arma 3 dedicated server command line.
 * -mod / -serverMod use absolute mod folder paths (like real start scripts).
 */
@Service
public class StartCommandService {
    private final ModRepository modRepository;
    private final ModLocalService modLocalService;

    public StartCommandService(ModRepository modRepository, ModLocalService modLocalService) {
        this.modRepository = modRepository;
        this.modLocalService = modLocalService;
    }

    public Path resolveGameExe(SettingService settingService) {
        Path dir = Path.of(settingService.gameServerDir());
        Path x64 = dir.resolve("arma3server_x64.exe");
        return java.nio.file.Files.exists(x64) ? x64 : dir.resolve("arma3server.exe");
    }

    /** Absolute folder path for a mod; prefer local scan result, then DB file_path. */
    public String resolveModPath(ModCatalog mod) {
        Path local = modLocalService.findLocalPathById(mod.id(), mod.workshopId(), null);
        if (local != null) {
            return local.toAbsolutePath().normalize().toString();
        }
        if (mod.filePath() != null && !mod.filePath().isBlank()) {
            return Path.of(mod.filePath()).toAbsolutePath().normalize().toString();
        }
        // fallback: typical steamcmd workshop layout under mods dir
        return Path.of(modLocalService.candidatesById(mod.id(), mod.workshopId(), null).get(0).toString())
                .toAbsolutePath().normalize().toString();
    }

    public List<String> modPaths(GameInstance inst, String loadType) {
        List<String> paths = new ArrayList<>();
        for (InstanceMod im : modRepository.findInstanceMods(inst.id())) {
            if (im.enabled() != 1 || !loadType.equals(im.loadType())) {
                continue;
            }
            ModCatalog mod = modRepository.findModById(im.modId()).orElse(null);
            if (mod != null) {
                paths.add(resolveModPath(mod));
            }
        }
        return paths;
    }

    public Map<String, Object> build(GameInstance inst, Path gameExe) {
        Path root = Path.of(inst.rootPath()).toAbsolutePath().normalize();
        Path serverCfg = root.resolve("server.cfg");
        Path basicCfg = root.resolve("basic.cfg");
        Path profiles = root.resolve("profiles");
        List<String> both = modPaths(inst, "BOTH");
        List<String> serverOnly = modPaths(inst, "SERVER_ONLY");

        List<String> args = new ArrayList<>();
        args.add(gameExe.toString());
        args.add("-filepatching");
        args.add("-port=" + (inst.gamePort() == null ? 2302 : inst.gamePort()));
        if (inst.bindIp() != null && !inst.bindIp().isBlank()) {
            args.add("-ip=" + inst.bindIp().trim());
        }
        args.add("-config=" + serverCfg);
        args.add("-cfg=" + basicCfg);
        args.add("-profiles=" + profiles);
        if (inst.profileName() != null && !inst.profileName().isBlank()) {
            args.add("-name=" + inst.profileName().trim());
        }
        if (!both.isEmpty()) {
            args.add("-mod=" + String.join(";", both) + ";");
        }
        if (!serverOnly.isEmpty()) {
            args.add("-serverMod=" + String.join(";", serverOnly) + ";");
        }
        if (inst.extraArgs() != null && !inst.extraArgs().isBlank()) {
            // custom command line fragments (may contain multiple args)
            for (String part : inst.extraArgs().trim().split("\\s+")) {
                if (!part.isBlank()) {
                    args.add(part);
                }
            }
        }

        // shell-like preview string with quotes around path-like values
        StringBuilder sb = new StringBuilder();
        sb.append(gameExe.getFileName().toString());
        for (int i = 1; i < args.size(); i++) {
            String a = args.get(i);
            sb.append(' ');
            if (a.contains("\\") || a.contains(" ")) {
                sb.append('"').append(a).append('"');
            } else {
                sb.append(a);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("instanceId", inst.id());
        result.put("command", sb.toString());
        result.put("args", args);
        result.put("serverCfgPath", serverCfg.toString());
        result.put("basicCfgPath", basicCfg.toString());
        result.put("rootPath", root.toString());
        result.put("mod", both);
        result.put("serverMod", serverOnly);
        result.put("bindIp", inst.bindIp());
        return result;
    }
}
