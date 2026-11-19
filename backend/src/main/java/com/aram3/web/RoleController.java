package com.aram3.web;

import com.aram3.common.ApiResponse;
import com.aram3.service.RoleAdminService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasAuthority('user:manage')")
public class RoleController {
    private final RoleAdminService roleAdminService;

    public RoleController(RoleAdminService roleAdminService) {
        this.roleAdminService = roleAdminService;
    }

    public record CreateRoleRequest(
            @NotBlank @Pattern(regexp = "[a-z0-9_]{2,32}") String code,
            @NotBlank String name,
            String description,
            List<String> perms
    ) {}

    public record UpdateRoleRequest(
            @NotBlank String name,
            String description,
            List<String> perms
    ) {}

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(roleAdminService.list());
    }

    @GetMapping("/perm-catalog")
    public ApiResponse<List<Map<String, Object>>> permCatalog() {
        return ApiResponse.ok(roleAdminService.permCatalog());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody CreateRoleRequest req) {
        return ApiResponse.ok(roleAdminService.create(req.code(), req.name(), req.description(), req.perms()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable long id, @RequestBody UpdateRoleRequest req) {
        return ApiResponse.ok(roleAdminService.update(id, req.name(), req.description(), req.perms()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        roleAdminService.delete(id);
        return ApiResponse.ok();
    }
}
