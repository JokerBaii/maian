package cn.maian.health.dto;

import cn.maian.health.domain.WearableDevice;

import java.time.Instant;
import java.util.UUID;

public record WearableDeviceResponse(
    UUID id,
    String deviceIdentifier,
    String name,
    String type,
    boolean connected,
    int battery,
    Instant lastSeenAt
) {
    public static WearableDeviceResponse from(WearableDevice device) {
        return new WearableDeviceResponse(
            device.getId(),
            device.getDeviceIdentifier(),
            device.getName(),
            device.getType(),
            device.isConnected(),
            device.getBattery() == null ? 0 : device.getBattery(),
            device.getLastSeenAt()
        );
    }

    public static WearableDeviceResponse unbound() {
        return new WearableDeviceResponse(null, "", "未绑定设备", "none", false, 0, null);
    }
}
