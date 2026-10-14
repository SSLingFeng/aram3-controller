package com.aram3.web;

import com.aram3.common.ApiResponse;
import com.aram3.domain.InstanceMod;
import com.aram3.domain.MapCatalog;
import com.aram3.domain.ModCatalog;
import com.aram3.repository.ModRepository;
import com.aram3.service.AssetService;
import com.aram3.service.ModImportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AssetController {
    private final AssetService assetService;
    private final ModRepository modRepository;
    private final ModImportService modImportService;

    public AssetController(AssetService assetService, ModRepository modRepository, ModImportService modImportService) {
        this.assetService = assetService;
        this.modRepository = modRepository;
        this.modImportService = modImportService;
    }

    @GetMapping("/maps")
    public ApiResponse<List<MapCatalog>> maps() {
        return ApiResponse.ok(assetService.listMaps());
    }

    @PostMapping("/maps")
    public ApiResponse<MapCatalog> uploadMap(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String mapKey,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(assetService.uploadMap(name, mapKey, file));
    }

    @DeleteMapping("/maps/{id}")
    public ApiResponse<Void> deleteMap(@PathVariable long id) {
        assetService.deleteMap(id);
        return ApiResponse.ok();
    }

    @PostMapping("/instances/{instanceId}/maps/{mapId}")
    public ApiResponse<MapCatalog> bindMap(
            @PathVariable long instanceId,
            @PathVariable long mapId,
            @RequestParam(defaultValue = "false") boolean active) {
        return ApiResponse.ok(assetService.bindMap(instanceId, mapId, active));
    }

    @GetMapping("/mods")
    public ApiResponse<List<Map<String, Object>>> mods() {
        return ApiResponse.ok(assetService.listMods());
    }

    @PostMapping("/mods")
    public ApiResponse<ModCatalog> createMod(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(assetService.createMod(
                body.get("name"), body.get("workshopId"), body.get("loadType")));
    }

    @PostMapping(value = "/mods/preset-import", consumes = "multipart/form-data")
    public ApiResponse<Map<String, Object>> importHtml(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(modImportService.importHtml(file));
    }

    @PostMapping("/mods/{id}/actions/download")
    public ApiResponse<Map<String, Object>> downloadMod(@PathVariable long id,
                                                       @RequestBody(required = false) Map<String, String> body) {
        String mode = body == null ? "DIRECT" : body.getOrDefault("mode", "DIRECT");
        return ApiResponse.ok(assetService.downloadMod(id, mode));
    }

    public record UpdateModsRequest(List<Long> modIds, Boolean all, String mode) {}

    @PostMapping("/mods/actions/update")
    public ApiResponse<Map<String, Object>> updateMods(@RequestBody UpdateModsRequest req) {
        boolean all = Boolean.TRUE.equals(req.all());
        return ApiResponse.ok(assetService.updateMods(req.modIds(), all, req.mode()));
    }

    @PostMapping("/mods/actions/local-search")
    public ApiResponse<Map<String, Object>> localSearch() {
        return ApiResponse.ok(assetService.localSearch());
    }

    public record InstanceModItem(Long modId, String loadType) {}

    public record SaveInstanceModsRequest(List<InstanceModItem> items) {}

    @PutMapping("/instances/{instanceId}/mods/load-list")
    public ApiResponse<List<InstanceMod>> saveLoadList(
            @PathVariable long instanceId,
            @RequestBody SaveInstanceModsRequest req) {
        java.util.Map<Long, String> desired = new java.util.HashMap<>();
        if (req.items() != null) {
            for (InstanceModItem item : req.items()) {
                if (item.modId() == null) {
                    continue;
                }
                String loadType = item.loadType();
                if ("BOTH".equals(loadType) || "SERVER_ONLY".equals(loadType)) {
                    desired.put(item.modId(), loadType);
                }
            }
        }
        // remove unbound
        for (InstanceMod existing : modRepository.findInstanceMods(instanceId)) {
            if (!desired.containsKey(existing.modId())) {
                assetService.unbindMod(instanceId, existing.modId());
            }
        }
        // upsert desired
        int order = 0;
        for (Map.Entry<Long, String> e : desired.entrySet()) {
            assetService.bindMod(instanceId, e.getKey(), e.getValue(), 1, order++);
        }
        return ApiResponse.ok(modRepository.findInstanceMods(instanceId));
    }

    @PostMapping("/instances/{instanceId}/mods")
    public ApiResponse<Void> bindMod(
            @PathVariable long instanceId,
            @RequestBody Map<String, Object> body) {
        long modId = Long.parseLong(String.valueOf(body.get("modId")));
        String loadType = String.valueOf(body.getOrDefault("loadType", "BOTH"));
        int enabled = Integer.parseInt(String.valueOf(body.getOrDefault("enabled", 1)));
        int loadOrder = Integer.parseInt(String.valueOf(body.getOrDefault("loadOrder", 0)));
        assetService.bindMod(instanceId, modId, loadType, enabled, loadOrder);
        return ApiResponse.ok();
    }

    @DeleteMapping("/instances/{instanceId}/mods/{modId}")
    public ApiResponse<Void> unbindMod(@PathVariable long instanceId, @PathVariable long modId) {
        assetService.unbindMod(instanceId, modId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/mods/{id}")
    public ApiResponse<Void> deleteMod(@PathVariable long id) {
        assetService.deleteMod(id);
        return ApiResponse.ok();
    }

    @GetMapping("/instances/{instanceId}/mods")
    public ApiResponse<List<InstanceMod>> instanceMods(@PathVariable long instanceId) {
        return ApiResponse.ok(modRepository.findInstanceMods(instanceId));
    }
}
