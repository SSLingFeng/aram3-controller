package com.aram3.web;

import com.aram3.common.ApiResponse;
import com.aram3.infra.steam.SteamCmdSessionManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/steamcmd")
public class SteamCmdController {
    private final SteamCmdSessionManager sessionManager;

    public SteamCmdController(SteamCmdSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public record StartRequest(
            String username,
            String password,
            String guardCode,
            String mode,
            Boolean all,
            List<Long> modIds
    ) {}

    public record CommandRequest(String command) {}

    private Long userId(Authentication auth) {
        return null;
    }

    @PostMapping("/sessions")
    public ApiResponse<Map<String, Object>> start(@RequestBody StartRequest req, Authentication auth) {
        var session = sessionManager.start(
                req.username(), req.password(), req.guardCode(), req.mode(),
                Boolean.TRUE.equals(req.all()), req.modIds(), userId(auth));
        return ApiResponse.ok(session.toView());
    }

    @GetMapping("/sessions")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(sessionManager.list());
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String id,
                                                @RequestParam(defaultValue = "0") int sinceSeq) {
        return ApiResponse.ok(sessionManager.logs(id, sinceSeq));
    }

    @PostMapping("/sessions/{id}/command")
    public ApiResponse<Map<String, Object>> command(@PathVariable String id,
                                                    @RequestBody CommandRequest req,
                                                    Authentication auth) {
        return ApiResponse.ok(sessionManager.sendCommand(id, req.command(), userId(auth)));
    }

    @PostMapping("/sessions/{id}/stop")
    public ApiResponse<Map<String, Object>> stop(@PathVariable String id, Authentication auth) {
        return ApiResponse.ok(sessionManager.stop(id, userId(auth)));
    }
}
