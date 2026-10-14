package com.aram3.web;

import com.aram3.common.ApiResponse;
import com.aram3.service.InstanceConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/instances/{id}/config-files")
public class InstanceConfigController {
    private final InstanceConfigService instanceConfigService;

    public InstanceConfigController(InstanceConfigService instanceConfigService) {
        this.instanceConfigService = instanceConfigService;
    }

    public record SaveConfigRequest(String serverCfg, String basicCfg) {}

    @GetMapping
    public ApiResponse<Map<String, Object>> get(@PathVariable long id) {
        return ApiResponse.ok(instanceConfigService.read(id));
    }

    @PutMapping
    public ApiResponse<Map<String, Object>> save(@PathVariable long id, @RequestBody SaveConfigRequest req) {
        return ApiResponse.ok(instanceConfigService.save(id, req.serverCfg(), req.basicCfg()));
    }

    @PostMapping("/reset")
    public ApiResponse<Map<String, Object>> reset(@PathVariable long id) {
        return ApiResponse.ok(instanceConfigService.reset(id));
    }
}
