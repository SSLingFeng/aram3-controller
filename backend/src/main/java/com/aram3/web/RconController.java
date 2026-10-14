package com.aram3.web;

import com.aram3.common.ApiResponse;
import com.aram3.domain.GameInstance;
import com.aram3.infra.rcon.RconService;
import com.aram3.service.InstanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/instances/{id}")
public class RconController {
    private final InstanceService instanceService;
    private final RconService rconService;

    public RconController(InstanceService instanceService, RconService rconService) {
        this.instanceService = instanceService;
        this.rconService = rconService;
    }

    @GetMapping("/players")
    public ApiResponse<List<String>> players(@PathVariable long id) {
        GameInstance inst = instanceService.get(id);
        return ApiResponse.ok(rconService.players(inst));
    }

    @PostMapping("/kick")
    public ApiResponse<Void> kick(@PathVariable long id, @RequestBody Map<String, Object> body) {
        GameInstance inst = instanceService.get(id);
        int playerId = Integer.parseInt(String.valueOf(body.get("playerId")));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        rconService.kick(inst, playerId, reason);
        return ApiResponse.ok();
    }

    @PostMapping("/broadcast")
    public ApiResponse<Void> broadcast(@PathVariable long id, @RequestBody Map<String, String> body) {
        GameInstance inst = instanceService.get(id);
        rconService.say(inst, -1, body.getOrDefault("message", ""));
        return ApiResponse.ok();
    }
}
