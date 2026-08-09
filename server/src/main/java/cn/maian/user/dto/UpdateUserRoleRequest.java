package cn.maian.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRoleRequest(
    @NotNull(message = "请选择账户角色")
    @Pattern(regexp = "USER|VOLUNTEER|ADMIN", message = "角色必须是 USER、VOLUNTEER 或 ADMIN")
    String role
) {
}
