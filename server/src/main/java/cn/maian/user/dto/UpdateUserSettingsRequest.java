package cn.maian.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateUserSettingsRequest(
    @NotNull Boolean rescuePush,
    @NotNull Boolean healthAlert,
    @NotNull Boolean scienceUpdate,
    @NotNull Boolean locationShare,
    @NotNull Boolean healthDataShare,
    @Min(80) @Max(240) int maxHeartRate,
    @Min(30) @Max(120) int minHeartRate
) {
}
