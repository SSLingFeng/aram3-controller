package com.aram3.web;

import com.aram3.common.ApiResponse;
import com.aram3.domain.GameInstance;
import com.aram3.domain.UserInstancePerm;
import com.aram3.service.InstanceService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/instances")
public class InstanceController {
    private final InstanceService instanceService;

    public InstanceController(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    public record CreateInstanceRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{1,64}") String code,
            @NotBlank String name,
            String description,
            Integer gamePort,
            Integer steamQueryPort,
            String bindIp,
            String rconHost,
            Integer rconPort,
            String rconPassword,
            Integer maxPlayers,
            String profileName,
            String startScript,
            String extraArgs
    ) {}

    public record PermRequest(
            long userId,
            int canView,
            int canStartStop,
            int canEditConfig,
            int canManageMap,
            int canManageMod,
            int canTransferFiles,
            int canViewLogs,
            int canRcon,
            int canSchedule
    ) {}

    @GetMapping
    public ApiResponse<List<GameInstance>> list() {
        return ApiResponse.ok(instanceService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<GameInstance> get(@PathVariable long id) {
        return ApiResponse.ok(instanceService.get(id));
    }

    @PostMapping
    public ApiResponse<GameInstance> create(@RequestBody CreateInstanceRequest req) {
        GameInstance draft = new GameInstance(
                null, req.code(), req.name(), req.description(), null,
                req.gamePort(), req.steamQueryPort(), req.bindIp(), req.rconHost(), req.rconPort(), req.rconPassword(),
                req.maxPlayers(), req.profileName(), req.startScript(), req.extraArgs(),
                "STOPPED", 1, null, null, null
        );
        return ApiResponse.ok(instanceService.create(draft, null));
    }

    @PutMapping("/{id}")
    public ApiResponse<GameInstance> update(@PathVariable long id, @RequestBody CreateInstanceRequest req) {
        GameInstance patch = new GameInstance(
                id, null, req.name(), req.description(), null,
                req.gamePort(), req.steamQueryPort(), req.bindIp(), req.rconHost(), req.rconPort(), req.rconPassword(),
                req.maxPlayers(), req.profileName(), req.startScript(), req.extraArgs(),
                null, 1, null, null, null
        );
        return ApiResponse.ok(instanceService.update(id, patch));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        instanceService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/start")
    public ApiResponse<Map<String, Object>> start(@PathVariable long id) {
        return ApiResponse.ok(Map.of("jobId", instanceService.startInstance(id)));
    }

    @PostMapping("/{id}/stop")
    public ApiResponse<Map<String, Object>> stop(@PathVariable long id) {
        return ApiResponse.ok(Map.of("jobId", instanceService.stopInstance(id)));
    }

    @GetMapping("/{id}/start-command-preview")
    public ApiResponse<Map<String, Object>> preview(@PathVariable long id) {
        return ApiResponse.ok(instanceService.buildStartCommandPreview(id));
    }

    @GetMapping("/{id}/permissions")
    public ApiResponse<List<UserInstancePerm>> permissions(@PathVariable long id) {
        return ApiResponse.ok(instanceService.listPerms(id));
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<Void> grant(@PathVariable long id, @RequestBody PermRequest req) {
        instanceService.grantPerm(new UserInstancePerm(
                null, req.userId(), id,
                req.canView(), req.canStartStop(), req.canEditConfig(),
                req.canManageMap(), req.canManageMod(), req.canTransferFiles(),
                req.canViewLogs(), req.canRcon(), req.canSchedule()
        ));
        return ApiResponse.ok();
    }
}
