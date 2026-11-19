package com.aram3.service;

import com.aram3.common.BusinessException;
import com.aram3.domain.GameInstance;
import com.aram3.domain.UserInstancePerm;
import com.aram3.repository.AuditRepository;
import com.aram3.repository.InstanceRepository;
import com.aram3.repository.ModRepository;
import com.aram3.repository.PermRepository;
import com.aram3.security.AuthFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InstanceService {
    private static final Logger log = LoggerFactory.getLogger(InstanceService.class);

    private final InstanceRepository instanceRepository;
    private final PermRepository permRepository;
    private final ModRepository modRepository;
    private final AuditRepository auditRepository;
    private final JobService jobService;
    private final SettingService settingService;
    private final InstanceConfigService instanceConfigService;
    private final StartCommandService startCommandService;

    public InstanceService(InstanceRepository instanceRepository,
                           PermRepository permRepository,
                           ModRepository modRepository,
                           AuditRepository auditRepository,
                           JobService jobService,
                           SettingService settingService,
                           InstanceConfigService instanceConfigService,
                           StartCommandService startCommandService) {
        this.instanceRepository = instanceRepository;
        this.permRepository = permRepository;
        this.modRepository = modRepository;
        this.auditRepository = auditRepository;
        this.jobService = jobService;
        this.settingService = settingService;
        this.instanceConfigService = instanceConfigService;
        this.startCommandService = startCommandService;
    }

    /** 当前用户可见的实例（超管全部；其余需 user_instance_perm.can_view=1）。 */
    public List<GameInstance> listVisible() {
        List<GameInstance> all = instanceRepository.findAll();
        if (AuthFacade.isSuperAdmin()) {
            return all;
        }
        Long uid = AuthFacade.currentUserId();
        if (uid == null) {
            return List.of();
        }
        Set<Long> viewable = permRepository.findByUser(uid).stream()
                .filter(UserInstancePerm::hasView)
                .map(UserInstancePerm::instanceId)
                .collect(Collectors.toSet());
        return all.stream().filter(i -> viewable.contains(i.id())).toList();
    }

    public GameInstance get(long id) {
        return instanceRepository.findById(id).orElseThrow(() -> new BusinessException("实例不存在"));
    }

    public GameInstance getByCode(String code) {
        return instanceRepository.findByCode(code).orElseThrow(() -> new BusinessException("实例不存在"));
    }

    /** 当前用户对该实例的有效权限（超管全开，无授权行则全零）。 */
    public UserInstancePerm effectivePerm(long instanceId) {
        if (AuthFacade.isSuperAdmin()) {
            return new UserInstancePerm(null, AuthFacade.currentUserId(), instanceId,
                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        }
        Long uid = AuthFacade.currentUserId();
        if (uid == null) {
            return emptyPerm(instanceId);
        }
        return permRepository.find(uid, instanceId).orElse(emptyPerm(instanceId));
    }

    private static UserInstancePerm emptyPerm(long instanceId) {
        return new UserInstancePerm(null, null, instanceId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    /** 实例级权限校验：超管放行；无授权行或对应开关为 0 时抛 403。 */
    public void requirePerm(long instanceId, Function<UserInstancePerm, Boolean> check) {
        if (AuthFacade.isSuperAdmin()) {
            return;
        }
        Long uid = AuthFacade.currentUserId();
        if (uid == null) {
            throw new BusinessException(403, "无权限操作该实例");
        }
        UserInstancePerm perm = permRepository.find(uid, instanceId).orElse(null);
        if (perm == null || !Boolean.TRUE.equals(check.apply(perm))) {
            throw new BusinessException(403, "无权限操作该实例");
        }
    }

    public void requireView(long instanceId) {
        requirePerm(instanceId, UserInstancePerm::hasView);
    }

    public void requireStartStop(long instanceId) {
        requirePerm(instanceId, UserInstancePerm::hasStartStop);
    }

    public void requireEditCore(long instanceId) {
        requirePerm(instanceId, UserInstancePerm::hasEditCore);
    }

    public void requireEditConfig(long instanceId) {
        requirePerm(instanceId, UserInstancePerm::hasEditConfig);
    }

    public void requireManageMap(long instanceId) {
        requirePerm(instanceId, UserInstancePerm::hasManageMap);
    }

    public void requireManageMod(long instanceId) {
        requirePerm(instanceId, UserInstancePerm::hasManageMod);
    }

    @Transactional
    public GameInstance create(GameInstance draft, Long userId) {
        if (instanceRepository.findByCode(draft.code()).isPresent()) {
            throw new BusinessException("实例 code 已存在");
        }
        Path root = resolveInstanceRoot(draft.code());
        try {
            Files.createDirectories(root);
            Files.createDirectories(root.resolve("profiles"));
            Files.createDirectories(root.resolve("missions"));
            Files.createDirectories(root.resolve("mod-links"));
        } catch (Exception e) {
            throw new BusinessException("创建实例目录失败: " + e.getMessage());
        }
        checkPortConflict(draft);
        GameInstance toSave = new GameInstance(
                null, draft.code(), draft.name(), draft.description(),
                root.toString(), draft.gamePort(), draft.steamQueryPort(),
                draft.bindIp(),
                draft.rconHost(), draft.rconPort(), draft.rconPassword(),
                draft.maxPlayers(), draft.profileName(),
                draft.startScript(), draft.extraArgs(),
                "STOPPED", 1, userId, null, null
        );
        long id = instanceRepository.create(toSave);
        auditRepository.log(userId, "instance.create", "instance", id, draft.code(), null);
        GameInstance created = get(id);
        try {
            instanceConfigService.ensureDefaultConfigs(created);
        } catch (Exception ignored) {
        }
        return created;
    }

    @Transactional
    public GameInstance update(long id, GameInstance patch) {
        GameInstance existing = get(id);
        GameInstance merged = new GameInstance(
                existing.id(), existing.code(),
                patch.name() != null ? patch.name() : existing.name(),
                patch.description() != null ? patch.description() : existing.description(),
                existing.rootPath(),
                patch.gamePort() != null ? patch.gamePort() : existing.gamePort(),
                patch.steamQueryPort() != null ? patch.steamQueryPort() : existing.steamQueryPort(),
                patch.bindIp() != null ? patch.bindIp() : existing.bindIp(),
                patch.rconHost() != null ? patch.rconHost() : existing.rconHost(),
                patch.rconPort() != null ? patch.rconPort() : existing.rconPort(),
                patch.rconPassword() != null ? patch.rconPassword() : existing.rconPassword(),
                patch.maxPlayers() != null ? patch.maxPlayers() : existing.maxPlayers(),
                patch.profileName() != null ? patch.profileName() : existing.profileName(),
                patch.startScript() != null ? patch.startScript() : existing.startScript(),
                patch.extraArgs() != null ? patch.extraArgs() : existing.extraArgs(),
                existing.status(),
                patch.enabled() != 0 ? patch.enabled() : existing.enabled(),
                existing.createdBy(), existing.createdAt(), null
        );
        checkPortConflict(merged);
        instanceRepository.update(merged);
        auditRepository.log(AuthFacade.currentUserId(), "instance.update", "instance", id, null, null);
        return get(id);
    }

    /** 删除实例：DB 记录 + 磁盘实例目录（须位于实例根目录下，防误删任意路径）。 */
    @Transactional
    public void delete(long id) {
        GameInstance inst = get(id);
        if ("RUNNING".equals(inst.status()) || "STARTING".equals(inst.status())) {
            throw new BusinessException("实例运行中，请先停止");
        }
        Path root = Path.of(inst.rootPath()).normalize();
        Path base = Path.of(settingService.instancesDir()).normalize();
        if (root.startsWith(base) && Files.exists(root)) {
            deleteRecursively(root);
        } else if (!root.startsWith(base)) {
            // rootPath 不在实例根目录下：只删记录，绝不删除任意目录
            log.warn("实例 {} 的 rootPath {} 不在实例根目录 {} 下，跳过磁盘删除",
                    inst.code(), root, base);
        }
        instanceRepository.delete(id);
        auditRepository.log(AuthFacade.currentUserId(), "instance.delete", "instance", id, inst.code(),
                root.toString());
    }

    /** 深度删除目录（不跟随符号链接，mod-links 中的链接只删链接本身）。 */
    private void deleteRecursively(Path dir) {
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new BusinessException("删除实例目录失败: " + p + " - " + e.getMessage());
                }
            });
        } catch (IOException e) {
            throw new BusinessException("删除实例目录失败: " + e.getMessage());
        }
    }

    public long startInstance(long id) {
        GameInstance inst = get(id);
        requireStartStop(inst.id());
        return jobService.createStartJob(inst, AuthFacade.currentUsername());
    }

    public long stopInstance(long id) {
        GameInstance inst = get(id);
        requireStartStop(inst.id());
        return jobService.createStopJob(inst, AuthFacade.currentUsername());
    }

    public Map<String, Object> buildStartCommandPreview(long id) {
        GameInstance inst = get(id);
        return startCommandService.build(inst, Path.of("arma3server_x64.exe"));
    }

    public void checkPortConflict(GameInstance draft) {
        for (GameInstance other : instanceRepository.findAll()) {
            if (draft.id() != null && other.id() != null && other.id().equals(draft.id())) {
                continue;
            }
            if (draft.gamePort() != null && draft.gamePort().equals(other.gamePort())) {
                throw new BusinessException("游戏端口 " + draft.gamePort() + " 与实例 " + other.code() + " 冲突");
            }
            if (draft.steamQueryPort() != null && draft.steamQueryPort().equals(other.steamQueryPort())) {
                throw new BusinessException("查询端口 " + draft.steamQueryPort() + " 与实例 " + other.code() + " 冲突");
            }
            if (draft.rconPort() != null && draft.rconPort().equals(other.rconPort())) {
                throw new BusinessException("RCON 端口 " + draft.rconPort() + " 与实例 " + other.code() + " 冲突");
            }
        }
    }

    private Path resolveInstanceRoot(String code) {
        if (code == null || !code.matches("[a-zA-Z0-9_-]{1,64}")) {
            throw new BusinessException("实例 code 仅允许字母数字下划线中划线");
        }
        return Path.of(settingService.instancesDir()).resolve(code).normalize();
    }
}
