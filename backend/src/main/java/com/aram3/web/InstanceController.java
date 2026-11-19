package com.aram3.web;

import com.aram3.common.ApiResponse;
import com.aram3.domain.GameInstance;
import com.aram3.domain.UserInstancePerm;
import com.aram3.security.AuthFacade;
import com.aram3.service.InstanceService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /** 当前用户对该实例的有效权限（随列表/详情返回，驱动前端按钮显隐）。 */
    public record MyPerms(
            boolean canView,
            boolean canStartStop,
            boolean canEditCore,
            boolean canEditConfig,
            boolean canManageMap,
            boolean canManageMod,
            boolean canTransferFiles,
            boolean canViewLogs,
            boolean canRcon,
            boolean canSchedule
    ) {}

    /** 实例视图：rconPassword 仅对持有核心配置权限的用户返回。 */
    public record InstanceView(
            Long id,
            String code,
            String name,
            String description,
            String rootPath,
            Integer gamePort,
            Integer steamQueryPort,
            String bindIp,
            String rconHost,
            Integer rconPort,
            String rconPassword,
            Integer maxPlayers,
            String profileName,
            String startScript,
            String extraArgs,
            String status,
            int enabled,
            MyPerms myPerms
    ) {}

    @GetMapping
    public ApiResponse<List<InstanceView>> list() {
        return ApiResponse.ok(instanceService.listVisible().stream().map(this::toView).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<InstanceView> get(@PathVariable long id) {
        instanceService.requireView(id);
        return ApiResponse.ok(toView(instanceService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('instance:create')")
    public ApiResponse<InstanceView> create(@RequestBody CreateInstanceRequest req) {
        GameInstance draft = new GameInstance(
                null, req.code(), req.name(), req.description(), null,
                req.gamePort(), req.steamQueryPort(), req.bindIp(), req.rconHost(), req.rconPort(), req.rconPassword(),
                req.maxPlayers(), req.profileName(), req.startScript(), req.extraArgs(),
                "STOPPED", 1, null, null, null
        );
        return ApiResponse.ok(toView(instanceService.create(draft, AuthFacade.currentUserId())));
    }

    @PutMapping("/{id}")
    public ApiResponse<InstanceView> update(@PathVariable long id, @RequestBody CreateInstanceRequest req) {
        instanceService.requireEditCore(id);
        GameInstance patch = new GameInstance(
                id, null, req.name(), req.description(), null,
                req.gamePort(), req.steamQueryPort(), req.bindIp(), req.rconHost(), req.rconPort(), req.rconPassword(),
                req.maxPlayers(), req.profileName(), req.startScript(), req.extraArgs(),
                null, 1, null, null, null
        );
        return ApiResponse.ok(toView(instanceService.update(id, patch)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable long id) {
        instanceService.requireEditCore(id);
        GameInstance inst = instanceService.get(id);
        instanceService.delete(id);
        return ApiResponse.ok(Map.of("deletedRootPath", inst.rootPath()));
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
        instanceService.requireView(id);
        return ApiResponse.ok(instanceService.buildStartCommandPreview(id));
    }

    private InstanceView toView(GameInstance g) {
        UserInstancePerm p = instanceService.effectivePerm(g.id());
        MyPerms mp = new MyPerms(
                p.hasView(), p.hasStartStop(), p.hasEditCore(), p.hasEditConfig(),
                p.hasManageMap(), p.hasManageMod(), p.hasTransferFiles(), p.hasViewLogs(),
                p.hasRcon(), p.hasSchedule()
        );
        return new InstanceView(
                g.id(), g.code(), g.name(), g.description(), g.rootPath(),
                g.gamePort(), g.steamQueryPort(), g.bindIp(), g.rconHost(), g.rconPort(),
                // RCON 密码只下发给能改核心配置的用户（RCON 连接由后端完成，前端无需明文）
                mp.canEditCore() ? g.rconPassword() : null,
                g.maxPlayers(), g.profileName(), g.startScript(), g.extraArgs(),
                g.status(), g.enabled(), mp
        );
    }
}
