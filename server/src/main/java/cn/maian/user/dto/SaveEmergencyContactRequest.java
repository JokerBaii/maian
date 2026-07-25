package cn.maian.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveEmergencyContactRequest(
    @NotBlank @Size(max = 60) String name,
    @NotBlank
    @Pattern(regexp = "^(1\\d{10}|\\d{3,4}-?\\d{7,8}|1\\d{2}\\*{4}\\d{4})$")
    String phone,
    @NotBlank @Size(max = 30) String relation
) {
}
