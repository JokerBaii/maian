package cn.maian.rescue.dto;

import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;
import cn.maian.rescue.domain.AedCustodyStatus;
import cn.maian.rescue.domain.RescueCall;

import java.time.Instant;
import java.util.UUID;

public record MatchedAedResponse(
    UUID deviceId,
    DeviceType type,
    String name,
    String category,
    String address,
    double longitude,
    double latitude,
    String contactPhoneMasked,
    String contactPhone,
    String vehicleInfo,
    int distanceMeters,
    int estimatedArrivalSeconds,
    String strategy,
    Instant matchedAt,
    AedCustodyStatus custodyStatus
) {
    public static MatchedAedResponse from(RescueCall rescueCall) {
        return from(rescueCall, false);
    }

    public static MatchedAedResponse from(RescueCall rescueCall, boolean revealContact) {
        EmergencyDevice device = rescueCall.getMatchedDevice();
        if (device == null) {
            return null;
        }
        return new MatchedAedResponse(
            device.getId(),
            device.getType(),
            device.getName(),
            device.getCategory(),
            rescueCall.getMatchedSnapshotAddress(),
            rescueCall.getMatchedSnapshotLongitude(),
            rescueCall.getMatchedSnapshotLatitude(),
            maskPhone(device.getOwnerPhone()),
            revealContact ? device.getOwnerPhone() : null,
            device.getVehicleInfo(),
            rescueCall.getMatchedDistanceMeters(),
            rescueCall.getEstimatedArrivalSeconds(),
            rescueCall.getMatchStrategy(),
            rescueCall.getMatchedAt(),
            rescueCall.getAedCustodyStatus()
        );
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.length() < 7) {
            return "****";
        }
        return trimmed.substring(0, 3) + "****" + trimmed.substring(trimmed.length() - 4);
    }
}
