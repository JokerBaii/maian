package cn.maian.user.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.common.api.PageResponse;
import cn.maian.user.dto.AdminUserResponse;
import cn.maian.user.dto.UpdateUserRoleRequest;
import cn.maian.user.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> findAll(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(adminUserService.findAll(pageable)));
    }

    @PatchMapping("/{id}/role")
    public ApiResponse<AdminUserResponse> changeRole(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return ApiResponse.ok(adminUserService.changeRole(id, request.role()));
    }
}
