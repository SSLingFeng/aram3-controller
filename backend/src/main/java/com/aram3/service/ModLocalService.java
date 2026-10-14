package com.aram3.service;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolve and check local Workshop mod files under the unified mods directory.
 */
@Service
public class ModLocalService {
    private final SettingService settingService;

    public ModLocalService(SettingService settingService) {
        this.settingService = settingService;
    }

    /** Candidate folders by workshop id (Steam naming). */
    public List<Path> candidates(String workshopId, Integer workshopAppId) {
        Path root = Path.of(settingService.modsDir()).toAbsolutePath().normalize();
        String appId = workshopAppId == null ? "107410" : String.valueOf(workshopAppId);
        List<Path> list = new ArrayList<>();
        if (workshopId != null && !workshopId.isBlank()) {
            list.add(root.resolve("steamapps/workshop/content").resolve(appId).resolve(workshopId));
            list.add(root.resolve("steamapps/workshop/content").resolve(workshopId));
            list.add(root.resolve("workshop/content").resolve(appId).resolve(workshopId));
            list.add(root.resolve(appId).resolve(workshopId));
            list.add(root.resolve(workshopId));
            list.add(root.resolve("@" + workshopId));
        }
        return list;
    }

    /** Also try folder names based on database id. */
    public List<Path> candidatesById(Long modId, String workshopId, Integer workshopAppId) {
        List<Path> list = new ArrayList<>(candidates(workshopId, workshopAppId));
        Path root = Path.of(settingService.modsDir()).toAbsolutePath().normalize();
        if (modId != null) {
            String id = String.valueOf(modId);
            list.add(root.resolve(id));
            list.add(root.resolve("@" + id));
            list.add(root.resolve("steamapps/workshop/content").resolve(id));
        }
        return list;
    }

    public Path findLocalPath(String workshopId, Integer workshopAppId) {
        for (Path p : candidates(workshopId, workshopAppId)) {
            if (Files.isDirectory(p) && hasContent(p)) {
                return p;
            }
        }
        return null;
    }

    public Path findLocalPathById(Long modId, String workshopId, Integer workshopAppId) {
        for (Path p : candidatesById(modId, workshopId, workshopAppId)) {
            if (Files.isDirectory(p) && hasContent(p)) {
                return p;
            }
        }
        return null;
    }

    public String localStatus(String workshopId, Integer workshopAppId) {
        return findLocalPath(workshopId, workshopAppId) != null ? "DOWNLOADED" : "MISSING";
    }

    public void deleteLocal(String workshopId, Integer workshopAppId) throws Exception {
        for (Path p : candidates(workshopId, workshopAppId)) {
            if (Files.exists(p)) {
                deleteRecursively(p);
            }
        }
    }

    public Map<String, Object> describe(String workshopId, Integer workshopAppId) {
        Path p = findLocalPath(workshopId, workshopAppId);
        return Map.of(
                "localStatus", p != null ? "DOWNLOADED" : "MISSING",
                "localPath", p == null ? "" : p.toString()
        );
    }

    public Map<String, Object> describeById(Long modId, String workshopId, Integer workshopAppId) {
        Path p = findLocalPathById(modId, workshopId, workshopAppId);
        return Map.of(
                "localStatus", p != null ? "DOWNLOADED" : "MISSING",
                "localPath", p == null ? "" : p.toString()
        );
    }

    private boolean hasContent(Path dir) {
        try (var s = Files.list(dir)) {
            return s.findAny().isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    private void deleteRecursively(Path path) throws Exception {
        if (Files.isDirectory(path)) {
            try (var walk = Files.walk(path)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            }
        } else {
            Files.deleteIfExists(path);
        }
    }
}
