package cn.maian.device.dto;

import cn.maian.device.domain.DeviceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDeviceStatusRequest(@NotNull DeviceStatus status) {
}
