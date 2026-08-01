package cn.maian.health.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveWearableDeviceRequest(
    @NotBlank @Size(max = 160) String deviceIdentifier,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 40) String type,
    boolean connected,
    @Min(0) @Max(100) Integer battery
) {
}
