package com.aram3.service;

import com.aram3.common.BusinessException;
import com.aram3.domain.ModCatalog;
import com.aram3.repository.AuditRepository;
import com.aram3.repository.ModRepository;
import com.aram3.repository.KeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * Arma 3 signature keys (*.bikey) → game server keys folder.
 */
@Service
public class KeyService {
    private final SettingService settingService;
    private final ModRepository modRepository;
    private final ModLocalService modLocalService;
    private final AuditRepository auditRepository;
    private final KeyRepository keyRepository;

    public KeyService(SettingService settingService,
                      ModRepository modRepository,
                      ModLocalService modLocalService,
                      AuditRepository auditRepository,
                      KeyRepository keyRepository) {
        this.settingService = settingService;
        this.modRepository = modRepository;
        this.modLocalService = modLocalService;
        this.auditRepository = auditRepository;
        this.keyRepository = keyRepository;
    }

    public Path keysDir() {
        return Path.of(settingService.gameServerDir(), "keys").toAbsolutePath().normalize();
    }

    public void ensureSchema() {
        try {
            keyRepository.ensureSchema();
        } catch (Exception ignored) {
        }
    }

    /** Scan selected (or all) mods for *.bikey and copy into keys folder. */
    public Map<String, Object> moveKeys(boolean all, List<Long> modIds) {
        keyRepository.ensureSchema();
        List<ModCatalog> targets = new ArrayList<>();
        if (all || modIds == null || modIds.isEmpty()) {
            targets.addAll(modRepository.findAllMods());
        } else {
            for (Long id : modIds) {
                modRepository.findModById(id).ifPresent(targets::add);
            }
        }
        if (targets.isEmpty()) {
            throw new BusinessException("没有可处理的模组");
        }

        Path dest = keysDir();
        try {
            Files.createDirectories(dest);
        } catch (IOException e) {
            throw new BusinessException("无法创建 keys 目录: " + dest + " / " + e.getMessage());
        }

        List<Map<String, Object>> copied = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        List<Map<String, Object>> renamed = new ArrayList<>();

        for (ModCatalog mod : targets) {
            Path modRoot = modLocalService.findLocalPathById(mod.id(), mod.workshopId(), null);
            if (modRoot == null) {
                Map<String, Object> miss = new LinkedHashMap<>();
                miss.put("mod", mod.name());
                miss.put("reason", "本地模组不存在");
                skipped.add(miss);
                continue;
            }
            List<Path> bikeys = findBikeys(modRoot);
            if (bikeys.isEmpty()) {
                Map<String, Object> miss = new LinkedHashMap<>();
                miss.put("mod", mod.name());
                miss.put("reason", "未找到 .bikey");
                skipped.add(miss);
                continue;
            }
            for (Path src : bikeys) {
                try {
                    String original = src.getFileName().toString();
                    String targetName = uniqueName(dest, original);
                    Path target = dest.resolve(targetName);
                    Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
                    upsertKey(targetName, target.toString(), mod.id(), mod.name());
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("mod", mod.name());
                    item.put("modId", mod.id());
                    item.put("source", src.toString());
                    item.put("file", targetName);
                    if (!targetName.equals(original)) {
                        item.put("renamedFrom", original);
                        renamed.add(item);
                    } else {
                        copied.add(item);
                    }
                } catch (Exception e) {
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("mod", mod.name());
                    err.put("file", src.getFileName().toString());
                    err.put("reason", e.getMessage());
                    skipped.add(err);
                }
            }
        }

        auditRepository.log(null, "key.move", "key", null,
                "copied=" + copied.size() + ",renamed=" + renamed.size() + ",skipped=" + skipped.size(), null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keysDir", dest.toString());
        result.put("copied", copied);
        result.put("renamed", renamed);
        result.put("skipped", skipped);
        result.put("copiedCount", copied.size());
        result.put("renamedCount", renamed.size());
        result.put("skippedCount", skipped.size());
        result.put("items", listKeys());
        return result;
    }

    public List<Map<String, Object>> listKeys() {
        keyRepository.ensureSchema();
        Path dest = keysDir();
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Map<String, Object>> meta = loadMeta();
        if (Files.isDirectory(dest)) {
            try (Stream<Path> s = Files.list(dest)) {
                s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".bikey"))
                        .sorted()
                        .forEach(p -> {
                            String name = p.getFileName().toString();
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("fileName", name);
                            row.put("filePath", p.toAbsolutePath().toString());
                            row.put("fileSize", fileSize(p));
                            Map<String, Object> m = meta.get(name);
                            if (m != null) {
                                row.put("sourceType", m.get("sourceType"));
                                row.put("sourceModId", m.get("sourceModId"));
                                row.put("sourceModName", m.get("sourceModName"));
                            } else {
                                row.put("sourceType", "UNKNOWN");
                                row.put("sourceModId", null);
                                row.put("sourceModName", "未知/手动");
                            }
                            rows.add(row);
                        });
            } catch (Exception e) {
                throw new BusinessException("读取 keys 目录失败: " + e.getMessage());
            }
        }
        return rows;
    }

    public void deleteKey(String fileName) {
        if (fileName == null || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new BusinessException("非法文件名");
        }
        if (!fileName.toLowerCase().endsWith(".bikey")) {
            throw new BusinessException("仅支持 .bikey 文件");
        }
        Path path = keysDir().resolve(fileName);
        try {
            Files.deleteIfExists(path);
            deleteMeta(fileName);
            auditRepository.log(null, "key.delete", "key", null, fileName, null);
        } catch (Exception e) {
            throw new BusinessException("删除失败: " + e.getMessage());
        }
    }

    public Map<String, Object> uploadKey(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择 .bikey 文件");
        }
        String original = file.getOriginalFilename() == null ? "manual.bikey" : file.getOriginalFilename();
        if (!original.toLowerCase().endsWith(".bikey")) {
            throw new BusinessException("仅支持 .bikey 文件");
        }
        try {
            Path dest = keysDir();
            Files.createDirectories(dest);
            String safe = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String targetName = uniqueName(dest, safe);
            Path target = dest.resolve(targetName);
            file.transferTo(target);
            upsertKey(targetName, target.toString(), null, "手动上传");
            auditRepository.log(null, "key.upload", "key", null, targetName, null);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fileName", targetName);
            result.put("items", listKeys());
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("上传失败: " + e.getMessage());
        }
    }

    private List<Path> findBikeys(Path modRoot) {
        List<Path> list = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(modRoot, 8)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".bikey"))
                    .forEach(list::add);
        } catch (Exception e) {
            // ignore partial walk
        }
        return list;
    }

    private String uniqueName(Path dir, String fileName) {
        if (!Files.exists(dir.resolve(fileName))) {
            return fileName;
        }
        String base = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            ext = fileName.substring(dot);
        }
        int i = 2;
        while (true) {
            String candidate = base + "_" + i + ext;
            if (!Files.exists(dir.resolve(candidate))) {
                return candidate;
            }
            i++;
            if (i > 9999) {
                return base + "_" + System.currentTimeMillis() + ext;
            }
        }
    }

    private void upsertKey(String fileName, String filePath, Long modId, String modName) {
        keyRepository.ensureSchema();
        String now = Instant.now().toString();
        String checksum = sha256(Path.of(filePath));
        int updated = updateMeta(fileName, filePath, checksum, modId == null ? "MANUAL" : "MOD", modId, modName, now);
        if (updated == 0) {
            insertMeta(fileName, filePath, checksum, modId == null ? "MANUAL" : "MOD", modId, modName, now);
        }
    }

    private Map<String, Map<String, Object>> loadMeta() {
        keyRepository.ensureSchema();
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        try {
            var rows = keyRepository.findAll();
            for (Map<String, Object> r : rows) {
                map.put(String.valueOf(r.get("file_name")), r);
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    private void insertMeta(String fileName, String filePath, String checksum, String sourceType, Long modId, String modName, String now) {
        keyRepository.insert(fileName, filePath, checksum, sourceType, modId, modName, now);
    }

    private int updateMeta(String fileName, String filePath, String checksum, String sourceType, Long modId, String modName, String now) {
        return keyRepository.update(fileName, filePath, checksum, sourceType, modId, modName, now);
    }

    private void deleteMeta(String fileName) {
        keyRepository.deleteByFileName(fileName);
    }

    private Long fileSize(Path p) {
        try {
            return Files.size(p);
        } catch (Exception e) {
            return null;
        }
    }

    private String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var in = Files.newInputStream(path)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    digest.update(buf, 0, n);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            return null;
        }
    }
}
