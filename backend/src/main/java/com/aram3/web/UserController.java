package com.aram3.web;

import com.aram3.common.ApiResponse;
import com.aram3.repository.UserRepository;
import com.aram3.service.UserAdminService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserAdminService userAdminService;
    private final UserRepository userRepository;

    public UserController(UserAdminService userAdminService, UserRepository userRepository) {
        this.userAdminService = userAdminService;
        this.userRepository = userRepository;
    }

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 64) String username,
            @NotBlank @Size(min = 6, max = 64) String password,
            String displayName,
            String email,
            boolean superAdmin,
            List<Long> roleIds
    ) {}

    public record UpdateUserRequest(
            String displayName,
            String email,
            Boolean superAdmin,
            List<Long> roleIds
    ) {}

    public record ResetPasswordRequest(@NotBlank @Size(min = 6, max = 64) String newPassword) {}

    public record ChangePasswordRequest(@NotBlank String oldPassword, @NotBlank String newPassword) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('admin','SUPER_ADMIN')")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(userAdminService.listUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin','SUPER_ADMIN')")
    public ApiResponse<Map<String, Object>> get(@PathVariable long id) {
        return ApiResponse.ok(userAdminService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('admin','SUPER_ADMIN')")
    public ApiResponse<Map<String, Object>> create(@RequestBody CreateUserRequest req) {
        return ApiResponse.ok(userAdminService.createUser(
                req.username(), req.password(), req.displayName(), req.email(),
                req.superAdmin(), req.roleIds()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin','SUPER_ADMIN')")
    public ApiResponse<Map<String, Object>> update(@PathVariable long id, @RequestBody UpdateUserRequest req) {
        return ApiResponse.ok(userAdminService.updateProfile(
                id, req.displayName(), req.email(), req.superAdmin(), req.roleIds()));
    }

    @PostMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('admin','SUPER_ADMIN')")
    public ApiResponse<Void> resetPassword(@PathVariable long id, @RequestBody ResetPasswordRequest req) {
        userAdminService.resetPassword(id, req.newPassword());
        return ApiResponse.ok();
    }

    /**
     * 解锁账户并重置密码（16 位随机，仅返回一次）——仅超级管理员可操作。
     */
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Map<String, Object>> unlock(@PathVariable long id) {
        return ApiResponse.ok(userAdminService.unlockAndResetPassword(id));
    }

    @PostMapping("/me/password")
    public ApiResponse<Void> changeOwnPassword(@RequestBody ChangePasswordRequest req) {
        userAdminService.changeOwnPassword(req.oldPassword(), req.newPassword());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('admin','SUPER_ADMIN')")
    public ApiResponse<Void> updateStatus(@PathVariable long id, @RequestParam int status) {
        userAdminService.updateStatus(id, status);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin','SUPER_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable long id) {
        userAdminService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('admin','SUPER_ADMIN')")
    public ApiResponse<List<Map<String, Object>>> roles() {
        return ApiResponse.ok(userRepository.findRoles());
    }
}
