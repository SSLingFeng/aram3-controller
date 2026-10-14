package com.aram3.web;

import com.aram3.common.ApiResponse;
import com.aram3.service.SettingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SettingController {
    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/settings")
    public ApiResponse<Map<String, Object>> get() {
        return ApiResponse.ok(settingService.getSettings());
    }

    @PutMapping("/settings")
    public ApiResponse<Map<String, Object>> update(@RequestBody Map<String, String> patch) {
        return ApiResponse.ok(settingService.updateSettings(patch));
    }
}
