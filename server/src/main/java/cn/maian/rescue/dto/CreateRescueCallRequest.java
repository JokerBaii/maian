package cn.maian.rescue.dto;

import cn.maian.rescue.domain.UrgencyLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

public record CreateRescueCallRequest(
    @NotNull UrgencyLevel urgency,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
    @NotBlank @Size(max = 255) String address,
    @Size(max = 1000) String description,
    @NotNull @Size(min = 1, max = 10) Set<@NotBlank @Size(max = 50) String> symptoms,
    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9._:-]+$")
    String clientRequestId
) {
}
