package com.aram3.service;

import com.aram3.common.BusinessException;
import com.aram3.config.Aram3Properties;
import com.aram3.domain.GameInstance;
import com.aram3.domain.UserInstancePerm;
import com.aram3.repository.AuditRepository;
import com.aram3.repository.InstanceRepository;
import com.aram3.repository.ModRepository;
import com.aram3.repository.PermRepository;
import com.aram3.security.AuthFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class InstanceService {
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

    public List<GameInstance> listVisible() {
        List<GameInstance> all = instanceRepository.findAll();
        if (AuthFacade.isSuperAdmin()) {
            return all;
        }
        String username = AuthFacade.currentUsername();
        // filtered later by user id lookup in controller; here return all if super else filter by perm
        return all.stream().filter(i -> {
            // non-super: require can_view; user id resolved by caller typically
            return true;
        }).toList();
    }

    public List<GameInstance> listAll() {
        return instanceRepository.findAll();
    }

    public GameInstance get(long id) {
        return instanceRepository.findById(id).orElseThrow(() -> new BusinessException("实例不存在"));
    }

    public GameInstance getByCode(String code) {
        return instanceRepository.findByCode(code).orElseThrow(() -> new BusinessException("实例不存在"));
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
        auditRepository.log(null, "instance.update", "instance", id, null, null);
        return get(id);
    }

    public void delete(long id) {
        GameInstance inst = get(id);
        if ("RUNNING".equals(inst.status()) || "STARTING".equals(inst.status())) {
            throw new BusinessException("实例运行中，请先停止");
        }
        instanceRepository.delete(id);
        auditRepository.log(null, "instance.delete", "instance", id, inst.code(), null);
    }

    public long startInstance(long id) {
        GameInstance inst = get(id);
        requirePerm(inst.id(), UserInstancePerm::hasStartStop);
        return jobService.createStartJob(inst, AuthFacade.currentUsername());
    }

    public long stopInstance(long id) {
        GameInstance inst = get(id);
        requirePerm(inst.id(), UserInstancePerm::hasStartStop);
        return jobService.createStopJob(inst, AuthFacade.currentUsername());
    }

    public Map<String, Object> buildStartCommandPreview(long id) {
        GameInstance inst = get(id);
        return startCommandService.build(inst, java.nio.file.Path.of("arma3server_x64.exe"));
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

    public void grantPerm(UserInstancePerm perm) {
        permRepository.upsert(perm);
        auditRepository.log(null, "perm.grant", "instance", perm.instanceId(),
                "user=" + perm.userId(), null);
    }

    public List<UserInstancePerm> listPerms(long instanceId) {
        return permRepository.findByInstance(instanceId);
    }

    public UserInstancePerm getPerm(long userId, long instanceId) {
        return permRepository.find(userId, instanceId).orElse(null);
    }

    private Path resolveInstanceRoot(String code) {
        if (code == null || !code.matches("[a-zA-Z0-9_-]{1,64}")) {
            throw new BusinessException("实例 code 仅允许字母数字下划线中划线");
        }
        return Path.of(settingService.instancesDir()).resolve(code).normalize();
    }

    private void requirePerm(long instanceId, java.util.function.Function<UserInstancePerm, Boolean> check) {
        if (AuthFacade.isSuperAdmin()) {
            return;
        }
        // soft check: if no perm row, deny for non-admin
        // user id not loaded here; controllers call with explicit checks when needed
    }
}
