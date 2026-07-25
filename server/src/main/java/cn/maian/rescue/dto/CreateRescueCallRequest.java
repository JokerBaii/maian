package cn.maian.rescue.dto;

import cn.maian.rescue.domain.UrgencyLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.List;

public record CreateRescueCallRequest(
    @NotNull UrgencyLevel urgency,
    @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
    @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
    @NotBlank @Size(max = 255) String address,
    @Size(max = 1000) String description,
    @NotNull @Size(min = 1, max = 10) Set<@NotBlank @Size(max = 50) String> symptoms,
    @Size(max = 9) List<@NotBlank @Size(max = 500) String> imageUrls
) {
    public CreateRescueCallRequest {
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }
}
