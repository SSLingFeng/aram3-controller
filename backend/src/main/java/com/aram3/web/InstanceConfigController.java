package com.aram3.web;

import com.aram3.common.ApiResponse;
import com.aram3.service.InstanceConfigService;
import com.aram3.service.InstanceService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/instances/{id}/config-files")
public class InstanceConfigController {
    private final InstanceConfigService instanceConfigService;
    private final InstanceService instanceService;

    public InstanceConfigController(InstanceConfigService instanceConfigService, InstanceService instanceService) {
        this.instanceConfigService = instanceConfigService;
        this.instanceService = instanceService;
    }

    public record SaveConfigRequest(String serverCfg, String basicCfg) {}

    @GetMapping
    public ApiResponse<Map<String, Object>> get(@PathVariable long id) {
        instanceService.requireView(id);
        return ApiResponse.ok(instanceConfigService.read(id));
    }

    @PutMapping
    public ApiResponse<Map<String, Object>> save(@PathVariable long id, @RequestBody SaveConfigRequest req) {
        instanceService.requireEditConfig(id);
        return ApiResponse.ok(instanceConfigService.save(id, req.serverCfg(), req.basicCfg()));
    }

    @PostMapping("/reset")
    public ApiResponse<Map<String, Object>> reset(@PathVariable long id) {
        instanceService.requireEditConfig(id);
        return ApiResponse.ok(instanceConfigService.reset(id));
    }
}
