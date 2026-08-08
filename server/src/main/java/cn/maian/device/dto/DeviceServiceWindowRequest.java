package cn.maian.device.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record DeviceServiceWindowRequest(
    @NotNull DayOfWeek dayOfWeek,
    @NotNull LocalTime opensAt,
    @NotNull LocalTime closesAt
) {
}
