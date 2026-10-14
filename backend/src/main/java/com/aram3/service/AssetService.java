package com.aram3.service;

import com.aram3.common.BusinessException;
import com.aram3.domain.MapCatalog;
import com.aram3.domain.ModCatalog;
import com.aram3.repository.AuditRepository;
import com.aram3.repository.MapRepository;
import com.aram3.repository.ModRepository;
import com.aram3.security.AuthFacade;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssetService {
    private final MapRepository mapRepository;
    private final ModRepository modRepository;
    private final AuditRepository auditRepository;
    private final JobService jobService;
    private final SettingService settingService;
    private final ModLocalService modLocalService;

    public AssetService(MapRepository mapRepository,
                        ModRepository modRepository,
                        AuditRepository auditRepository,
                        JobService jobService,
                        SettingService settingService,
                        ModLocalService modLocalService) {
        this.mapRepository = mapRepository;
        this.modRepository = modRepository;
        this.auditRepository = auditRepository;
        this.jobService = jobService;
        this.settingService = settingService;
        this.modLocalService = modLocalService;
    }

    // ----- maps -----

    public List<MapCatalog> listMaps() {
        return mapRepository.findAll();
    }

    public MapCatalog uploadMap(String name, String mapKey, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件为空");
        }
        String original = file.getOriginalFilename() == null ? "map.pbo" : file.getOriginalFilename();
        if (!original.toLowerCase().endsWith(".pbo")) {
            throw new BusinessException("地图仅支持 .pbo 文件");
        }
        try {
            Path mapsDir = Path.of(settingService.mpmissionsDir());
            Files.createDirectories(mapsDir);
            String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String derivedKey = safeName.replaceAll("\\.[^.]+$", "");
            if (mapKey == null || mapKey.isBlank()) {
                mapKey = derivedKey;
            }
            if (name == null || name.isBlank()) {
                name = derivedKey;
            }
            Path target = mapsDir.resolve(System.currentTimeMillis() + "-" + safeName);
            file.transferTo(target);
            String checksum = sha256(target);
            long id = mapRepository.create(new MapCatalog(
                    null,
                    name == null || name.isBlank() ? safeName : name,
                    mapKey,
                    safeName,
                    target.toString(),
                    Files.size(target),
                    checksum,
                    "upload",
                    currentUserId(),
                    null, null
            ));
            auditRepository.log(currentUserId(), "map.upload", "map", id, safeName, null);
            return mapRepository.findById(id).orElseThrow();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("上传失败: " + e.getMessage());
        }
    }

    public void deleteMap(long id) {
        MapCatalog map = mapRepository.findById(id).orElseThrow(() -> new BusinessException("地图不存在"));
        mapRepository.delete(id);
        try {
            Files.deleteIfExists(Path.of(map.filePath()));
        } catch (Exception ignored) {
        }
        auditRepository.log(currentUserId(), "map.delete", "map", id, map.name(), null);
    }

    public MapCatalog bindMap(long instanceId, long mapId, boolean active) {
        mapRepository.bindInstanceMap(instanceId, mapId, active, 0);
        auditRepository.log(currentUserId(), "map.bind", "instance", instanceId, "map=" + mapId, null);
        return mapRepository.findById(mapId).orElseThrow();
    }

    // ----- mods -----

    public List<Map<String, Object>> listMods() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ModCatalog m : modRepository.findAllMods()) {
            result.add(toModView(m));
        }
        return result;
    }

    public Map<String, Object> toModView(ModCatalog m) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", m.id());
        v.put("name", m.name());
        v.put("workshopId", m.workshopId());
        v.put("author", m.author());
        v.put("version", m.version());
        v.put("fileName", m.fileName());
        v.put("filePath", m.filePath());
        v.put("fileSize", m.fileSize());
        v.put("defaultLoadType", m.defaultLoadType());
        v.put("downloadStatus", m.downloadStatus());
        v.put("description", m.description());
        v.put("createdAt", m.createdAt());
        v.put("updatedAt", m.updatedAt());
        Map<String, Object> local = modLocalService.describeById(m.id(), m.workshopId(), null);
        v.put("localStatus", local.get("localStatus"));
        v.put("localPath", local.get("localPath"));
        return v;
    }

    /** Import / create without auto download. */
    public ModCatalog createMod(String name, String workshopId, String loadType) {
        return createMod(name, workshopId, loadType, false);
    }

    public ModCatalog createMod(String name, String workshopId, String loadType, boolean autoDownload) {
        if (workshopId == null || !workshopId.matches("\\d{5,12}")) {
            throw new BusinessException("Workshop ID 无效");
        }
        if (modRepository.findModByWorkshopId(workshopId).isPresent()) {
            throw new BusinessException("该 Workshop 模组已存在");
        }
        long id = modRepository.createMod(new ModCatalog(
                null,
                name == null || name.isBlank() ? ("Workshop " + workshopId) : name,
                workshopId,
                null, null, null, null, null, null,
                loadType == null ? "BOTH" : loadType,
                "PENDING",
                null,
                currentUserId(),
                null, null
        ));
        auditRepository.log(currentUserId(), "mod.create", "mod", id, workshopId, null);
        if (autoDownload) {
            jobService.createWorkshopJob(id, workshopId, AuthFacade.currentUsername(), "DIRECT");
        }
        return modRepository.findModById(id).orElseThrow();
    }

    public Map<String, Object> downloadMod(long id) {
        return downloadMod(id, "DIRECT");
    }

    public Map<String, Object> downloadMod(long id, String mode) {
        ModCatalog mod = modRepository.findModById(id)
                .orElseThrow(() -> new BusinessException("模组不存在"));
        String m = "REDOWNLOAD".equalsIgnoreCase(mode) ? "REDOWNLOAD" : "DIRECT";
        long jobId = jobService.createWorkshopJob(mod.id(), mod.workshopId(), AuthFacade.currentUsername(), m);
        return Map.of("jobId", jobId, "workshopId", mod.workshopId(), "mode", m);
    }

    /** Rescan local folders for every mod. Returns items with localStatus + id. */
    public Map<String, Object> localSearch() {
        List<Map<String, Object>> items = new ArrayList<>();
        int ok = 0;
        int missing = 0;
        for (ModCatalog m : modRepository.findAllMods()) {
            Map<String, Object> local = modLocalService.describeById(m.id(), m.workshopId(), null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.id());
            item.put("name", m.name());
            item.put("workshopId", m.workshopId());
            item.put("localStatus", local.get("localStatus"));
            item.put("localPath", local.get("localPath"));
            boolean exists = "DOWNLOADED".equals(local.get("localStatus"));
            item.put("scanStatus", exists ? "OK" : "MISSING");
            if (exists) {
                ok++;
            } else {
                missing++;
            }
            items.add(item);
        }
        auditRepository.log(currentUserId(), "mod.local_search", "mod", null,
                "ok=" + ok + ",missing=" + missing, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", ok);
        result.put("missing", missing);
        result.put("total", items.size());
        result.put("items", items);
        return result;
    }

    /**
     * Batch update. all=true -> every mod; otherwise only modIds.
     * mode: DIRECT | REDOWNLOAD (delete local files then download again).
     */
    public Map<String, Object> updateMods(List<Long> modIds, boolean all, String mode) {
        String m = "REDOWNLOAD".equalsIgnoreCase(mode) ? "REDOWNLOAD" : "DIRECT";
        List<ModCatalog> targets = new ArrayList<>();
        if (all) {
            targets.addAll(modRepository.findAllMods());
        } else if (modIds != null) {
            for (Long id : modIds) {
                modRepository.findModById(id).ifPresent(targets::add);
            }
        }
        if (targets.isEmpty()) {
            throw new BusinessException("请选择要更新的模组");
        }
        List<Long> jobs = new ArrayList<>();
        for (ModCatalog mod : targets) {
            long jobId = jobService.createWorkshopJob(
                    mod.id(), mod.workshopId(), AuthFacade.currentUsername(), m);
            jobs.add(jobId);
        }
        auditRepository.log(currentUserId(), "mod.update_batch", "mod", null,
                "count=" + jobs.size() + ",mode=" + m, null);
        return Map.of("mode", m, "jobIds", jobs, "count", jobs.size());
    }

    public void bindMod(long instanceId, long modId, String loadType, int enabled, int loadOrder) {
        if (!"SERVER_ONLY".equals(loadType) && !"BOTH".equals(loadType)) {
            throw new BusinessException("load_type 仅支持 SERVER_ONLY / BOTH");
        }
        modRepository.bindInstanceMod(instanceId, modId, loadType, enabled, loadOrder);
        auditRepository.log(currentUserId(), "mod.bind", "instance", instanceId,
                "mod=" + modId + ",load=" + loadType, null);
    }

    public void unbindMod(long instanceId, long modId) {
        modRepository.unbindInstanceMod(instanceId, modId);
        auditRepository.log(currentUserId(), "mod.unbind", "instance", instanceId, "mod=" + modId, null);
    }

    public void deleteMod(long id) {
        int refs = modRepository.countModReferences(id);
        if (refs > 0) {
            throw new BusinessException("模组已被 " + refs + " 个实例引用，请先解绑");
        }
        modRepository.deleteMod(id);
        auditRepository.log(currentUserId(), "mod.delete", "mod", id, null, null);
    }

    private Long currentUserId() {
        return null;
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var in = Files.newInputStream(path)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                digest.update(buf, 0, n);
            }
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }
}
