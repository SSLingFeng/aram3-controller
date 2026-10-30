package com.aram3.web;

import com.aram3.common.ApiResponse;
import com.aram3.common.BusinessException;
import com.aram3.domain.GameInstance;
import com.aram3.domain.UserInstancePerm;
import com.aram3.infra.rcon.BanEntry;
import com.aram3.infra.rcon.BanFileService;
import com.aram3.infra.rcon.PlayerInfo;
import com.aram3.infra.rcon.RconService;
import com.aram3.repository.AuditRepository;
import com.aram3.repository.PermRepository;
import com.aram3.repository.UserRepository;
import com.aram3.security.AuthFacade;
import com.aram3.service.InstanceService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行中实例的 BattlEye RCON 实时控制：玩家管理、公告、封禁、服务器控制、任务切换。
 */
@RestController
@RequestMapping("/api/v1/instances/{id}/rcon")
public class RconController {

    private final InstanceService instanceService;
    private final RconService rconService;
    private final BanFileService banFileService;
    private final PermRepository permRepository;
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;

    public RconController(InstanceService instanceService, RconService rconService,
                          BanFileService banFileService, PermRepository permRepository,
                          UserRepository userRepository, AuditRepository auditRepository) {
        this.instanceService = instanceService;
        this.rconService = rconService;
        this.banFileService = banFileService;
        this.permRepository = permRepository;
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
    }

    // ---------------------------------------------------------------- 玩家

    @GetMapping("/players")
    public ApiResponse<List<PlayerInfo>> players(@PathVariable long id) {
        return ApiResponse.ok(rconService.players(requireRcon(id)));
    }

    @PostMapping("/kick")
    public ApiResponse<String> kick(@PathVariable long id, @RequestBody KickReq req) {
        GameInstance inst = requireRcon(id);
        String result = rconService.kick(inst, req.playerNum(), req.reason());
        audit(id, "rcon.kick", "playerNum=" + req.playerNum() + ", reason=" + req.reason());
        return ApiResponse.ok(result);
    }

    @PostMapping("/ban")
    public ApiResponse<String> ban(@PathVariable long id, @RequestBody BanReq req) {
        GameInstance inst = requireRcon(id);
        String result = rconService.ban(inst, req.playerNum(), req.minutes(), req.reason());
        audit(id, "rcon.ban", "playerNum=" + req.playerNum() + ", minutes=" + req.minutes() + ", reason=" + req.reason());
        return ApiResponse.ok(result);
    }

    @PostMapping("/ban-player")
    public ApiResponse<String> banPlayer(@PathVariable long id, @RequestBody BanPlayerReq req) {
        GameInstance inst = requireRcon(id);
        String result = rconService.banPlayer(inst, req.target(), req.minutes(), req.reason());
        audit(id, "rcon.banPlayer", "target=" + req.target() + ", minutes=" + req.minutes());
        return ApiResponse.ok(result);
    }

    // ---------------------------------------------------------------- 公告与私聊

    @PostMapping("/say")
    public ApiResponse<String> say(@PathVariable long id, @RequestBody SayReq req) {
        GameInstance inst = requireRcon(id);
        String result = rconService.say(inst, req.message());
        audit(id, "rcon.say", req.message());
        return ApiResponse.ok(result);
    }

    @PostMapping("/say-to")
    public ApiResponse<String> sayTo(@PathVariable long id, @RequestBody SayToReq req) {
        GameInstance inst = requireRcon(id);
        String result = rconService.sayTo(inst, req.playerNum(), req.message());
        audit(id, "rcon.sayTo", "playerNum=" + req.playerNum() + ", message=" + req.message());
        return ApiResponse.ok(result);
    }

    // ---------------------------------------------------------------- 封禁管理

    /** RCON bans 命令原始输出（部分 BEServer 支持）。 */
    @GetMapping("/bans")
    public ApiResponse<String> bans(@PathVariable long id) {
        return ApiResponse.ok(rconService.bans(requireRcon(id)));
    }

    /** 本地 bans.txt 封禁列表（协议文档 2.4 节）。 */
    @GetMapping("/bans-file")
    public ApiResponse<Map<String, Object>> bansFile(@PathVariable long id) {
        GameInstance inst = requireRcon(id);
        var file = banFileService.findBansFile(inst);
        List<BanEntry> entries = banFileService.listBans(inst);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("path", file == null ? null : file.toString());
        out.put("entries", entries);
        return ApiResponse.ok(out);
    }

    /** 解封：删除 bans.txt 对应行并重新加载封禁列表。 */
    @PostMapping("/bans-file/unban")
    public ApiResponse<Map<String, Object>> unban(@PathVariable long id, @RequestBody UnbanReq req) {
        GameInstance inst = requireRcon(id);
        var file = banFileService.removeBan(inst, req.lineNo());
        String reload = safeLoadBans(inst);
        audit(id, "rcon.unban", "lineNo=" + req.lineNo() + ", file=" + file);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("path", file.toString());
        out.put("reload", reload);
        out.put("entries", banFileService.listBans(inst));
        return ApiResponse.ok(out);
    }

    @PostMapping("/remove-ban")
    public ApiResponse<String> removeBan(@PathVariable long id, @RequestBody RemoveBanReq req) {
        GameInstance inst = requireRcon(id);
        String result = rconService.removeBan(inst, req.banId());
        audit(id, "rcon.removeBan", "banId=" + req.banId());
        return ApiResponse.ok(result);
    }

    @PostMapping("/load-bans")
    public ApiResponse<String> loadBans(@PathVariable long id) {
        return ApiResponse.ok(rconService.loadBans(requireRcon(id)));
    }

    // ---------------------------------------------------------------- 服务器控制

    @PostMapping("/lock")
    public ApiResponse<String> lock(@PathVariable long id) {
        GameInstance inst = requireRcon(id);
        audit(id, "rcon.lock", null);
        return ApiResponse.ok(rconService.lock(inst));
    }

    @PostMapping("/unlock")
    public ApiResponse<String> unlock(@PathVariable long id) {
        GameInstance inst = requireRcon(id);
        audit(id, "rcon.unlock", null);
        return ApiResponse.ok(rconService.unlock(inst));
    }

    @PostMapping("/max-players")
    public ApiResponse<String> maxPlayers(@PathVariable long id, @RequestBody MaxPlayersReq req) {
        GameInstance inst = requireRcon(id);
        audit(id, "rcon.maxPlayers", "value=" + req.value());
        return ApiResponse.ok(rconService.maxPlayers(inst, req.value()));
    }

    @PostMapping("/reassign")
    public ApiResponse<String> reassign(@PathVariable long id) {
        GameInstance inst = requireRcon(id);
        audit(id, "rcon.reassign", null);
        return ApiResponse.ok(rconService.reassign(inst));
    }

    @PostMapping("/restart-server")
    public ApiResponse<String> restartServer(@PathVariable long id) {
        GameInstance inst = requireRcon(id);
        audit(id, "rcon.restartServer", null);
        return ApiResponse.ok(rconService.restartServer(inst));
    }

    @PostMapping("/restart")
    public ApiResponse<String> restart(@PathVariable long id) {
        GameInstance inst = requireRcon(id);
        audit(id, "rcon.restart", null);
        return ApiResponse.ok(rconService.restart(inst));
    }

    @PostMapping("/restart-time")
    public ApiResponse<String> restartTime(@PathVariable long id, @RequestBody RestartTimeReq req) {
        GameInstance inst = requireRcon(id);
        audit(id, "rcon.restartTime", "minutes=" + req.minutes() + ", seconds=" + req.seconds());
        return ApiResponse.ok(rconService.restartTime(inst, req.minutes(), req.seconds()));
    }

    /** 关停服务器——仅超管。 */
    @PostMapping("/shutdown")
    public ApiResponse<String> shutdown(@PathVariable long id) {
        requireSuperAdmin();
        GameInstance inst = instanceService.get(id);
        audit(id, "rcon.shutdown", null);
        return ApiResponse.ok(rconService.shutdown(inst));
    }

    // ---------------------------------------------------------------- 任务与事件

    @GetMapping("/missions")
    public ApiResponse<String> missions(@PathVariable long id) {
        return ApiResponse.ok(rconService.missions(requireRcon(id)));
    }

    @PostMapping("/mission")
    public ApiResponse<String> mission(@PathVariable long id, @RequestBody MissionReq req) {
        GameInstance inst = requireRcon(id);
        audit(id, "rcon.mission", "name=" + req.name());
        return ApiResponse.ok(rconService.mission(inst, req.name()));
    }

    @GetMapping("/events")
    public ApiResponse<String> events(@PathVariable long id) {
        return ApiResponse.ok(rconService.events(requireRcon(id)));
    }

    @PostMapping("/load-events")
    public ApiResponse<String> loadEvents(@PathVariable long id) {
        return ApiResponse.ok(rconService.loadEvents(requireRcon(id)));
    }

    @PostMapping("/load-scripts")
    public ApiResponse<String> loadScripts(@PathVariable long id) {
        return ApiResponse.ok(rconService.loadScripts(requireRcon(id)));
    }

    @PostMapping("/load-rpt")
    public ApiResponse<String> loadRpt(@PathVariable long id) {
        return ApiResponse.ok(rconService.loadRPT(requireRcon(id)));
    }

    @GetMapping("/rpt-tail")
    public ApiResponse<BanFileService.RptTail> rptTail(@PathVariable long id,
                                                       @RequestParam(defaultValue = "200") int lines) {
        return ApiResponse.ok(banFileService.rptTail(requireRcon(id), lines));
    }

    /** 原始 RCON 命令——仅超管。 */
    @PostMapping("/exec")
    public ApiResponse<String> exec(@PathVariable long id, @RequestBody ExecReq req) {
        requireSuperAdmin();
        GameInstance inst = instanceService.get(id);
        audit(id, "rcon.exec", req.command());
        return ApiResponse.ok(rconService.exec(inst, req.command()));
    }

    // ---------------------------------------------------------------- 权限与审计

    private GameInstance requireRcon(long id) {
        GameInstance inst = instanceService.get(id);
        if (AuthFacade.isSuperAdmin()) {
            return inst;
        }
        UserInstancePerm perm = permRepository.find(currentUserId(), id).orElse(null);
        if (perm == null || !perm.hasRcon()) {
            throw new BusinessException(403, "无权操作该实例的 RCON 控制台");
        }
        return inst;
    }

    private void requireSuperAdmin() {
        if (!AuthFacade.isSuperAdmin()) {
            throw new BusinessException(403, "该操作仅限超级管理员");
        }
    }

    private long currentUserId() {
        return userRepository.findByUsername(AuthFacade.currentUsername())
                .orElseThrow(() -> new BusinessException(401, "用户不存在"))
                .id();
    }

    private String safeLoadBans(GameInstance inst) {
        try {
            return rconService.loadBans(inst);
        } catch (BusinessException e) {
            return "封禁文件已更新，但 loadBans 失败：" + e.getMessage();
        }
    }

    private void audit(long instanceId, String action, String detail) {
        Long userId = null;
        try {
            userId = currentUserId();
        } catch (BusinessException ignored) {
        }
        auditRepository.log(userId, action, "instance", instanceId, detail, null);
    }
}

record KickReq(int playerNum, String reason) {
}

record BanReq(int playerNum, int minutes, String reason) {
}

record BanPlayerReq(String target, int minutes, String reason) {
}

record RemoveBanReq(String banId) {
}

record UnbanReq(int lineNo) {
}

record SayReq(String message) {
}

record SayToReq(int playerNum, String message) {
}

record MaxPlayersReq(int value) {
}

record RestartTimeReq(int minutes, int seconds) {
}

record MissionReq(String name) {
}

record ExecReq(String command) {
}
