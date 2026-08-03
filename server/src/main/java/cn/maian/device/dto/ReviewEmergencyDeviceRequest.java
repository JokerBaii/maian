package cn.maian.device.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewEmergencyDeviceRequest(
    @NotNull Boolean approved,
    @Size(max = 300) String reviewNote
) {
}
