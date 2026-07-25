package cn.maian.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyIdentityRequest(
    @NotBlank @Size(max = 60) String realName,
    @NotBlank
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$")
    String idCard
) {
}
