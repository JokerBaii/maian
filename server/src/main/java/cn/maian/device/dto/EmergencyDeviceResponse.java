package cn.maian.device.dto;

import cn.maian.device.domain.DeviceStatus;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EmergencyDeviceResponse(
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
    LocalDate expireDate,
    String owner,
    String vehicleInfo,
    Integer serviceRange,
    String instructions,
    List<String> imageUrls,
    List<String> vehicleImageUrls,
    Instant lastLocationAt,
    String reviewNote,
    Instant reviewedAt,
    Instant createdAt
) {
    public static EmergencyDeviceResponse from(EmergencyDevice device) {
        return new EmergencyDeviceResponse(
            device.getId(),
            device.getType(),
            device.getCategory(),
            device.getName(),
            device.getAddress(),
            device.getLongitude(),
            device.getLatitude(),
            device.getStatus(),
            device.getOwnerPhone(),
            device.getServiceTime(),
            device.getExpireDate(),
            device.getOwner(),
            device.getVehicleInfo(),
            device.getServiceRange(),
            device.getInstructions(),
            device.getImageUrls(),
            device.getVehicleImageUrls(),
            device.getLastLocationAt(),
            device.getReviewNote(),
            device.getReviewedAt(),
            device.getCreatedAt()
        );
    }
}
