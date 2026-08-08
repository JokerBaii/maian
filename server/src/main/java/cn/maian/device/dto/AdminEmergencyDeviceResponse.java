package cn.maian.device.dto;

import cn.maian.device.domain.DeviceStatus;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;
import cn.maian.device.domain.MobilePresenceStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminEmergencyDeviceResponse(
    UUID id,
    DeviceType type,
    String category,
    String name,
    String address,
    double longitude,
    double latitude,
    DeviceStatus status,
    String ownerPhone,
    String serviceTime,
    List<DeviceServiceWindowResponse> serviceWindows,
    LocalDate expireDate,
    String owner,
    String vehicleInfo,
    Integer serviceRange,
    String instructions,
    List<UUID> imageMediaIds,
    List<UUID> vehicleImageMediaIds,
    Instant lastLocationAt,
    UUID registeredByUserId,
    UUID reservedForCallId,
    Instant reservedAt,
    String reviewNote,
    Instant reviewedAt,
    Instant createdAt,
    MobilePresenceStatus mobilePresenceStatus
) {
    public static AdminEmergencyDeviceResponse from(EmergencyDevice device, Instant now, long freshSeconds) {
        return new AdminEmergencyDeviceResponse(
            device.getId(), device.getType(), device.getCategory(), device.getName(),
            device.getAddress(), device.getLongitude(), device.getLatitude(), device.getStatus(),
            device.getOwnerPhone(), DeviceSchedulePresenter.display(device.getServiceWindows()),
            DeviceSchedulePresenter.windows(device.getServiceWindows()), device.getExpireDate(),
            device.getOwner(), device.getVehicleInfo(), device.getServiceRange(),
            device.getInstructions(), device.getImageMediaIds(), device.getVehicleImageMediaIds(),
            device.getLastLocationAt(), device.getRegisteredByUserId(), device.getReservedForCallId(),
            device.getReservedAt(), device.getReviewNote(), device.getReviewedAt(), device.getCreatedAt(),
            device.mobilePresenceAt(now, freshSeconds)
        );
    }
}
