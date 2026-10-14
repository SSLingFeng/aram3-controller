package com.aram3.web;

import com.aram3.common.ApiResponse;
import com.aram3.service.KeyService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class KeyController {
    private final KeyService keyService;

    public KeyController(KeyService keyService) {
        this.keyService = keyService;
    }

    public record MoveKeysRequest(Boolean all, List<Long> modIds) {}

    @GetMapping("/keys")
    public ApiResponse<List<Map<String, Object>>> listKeys() {
        return ApiResponse.ok(keyService.listKeys());
    }

    @PostMapping("/mods/actions/move-keys")
    public ApiResponse<Map<String, Object>> moveKeys(@RequestBody(required = false) MoveKeysRequest req) {
        boolean all = req == null || !Boolean.FALSE.equals(req.all());
        List<Long> ids = req == null ? null : req.modIds();
        return ApiResponse.ok(keyService.moveKeys(all, ids));
    }

    @DeleteMapping("/keys")
    public ApiResponse<Void> deleteKey(@RequestParam String fileName) {
        keyService.deleteKey(fileName);
        return ApiResponse.ok();
    }

    @PostMapping("/keys/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(keyService.uploadKey(file));
    }
}
