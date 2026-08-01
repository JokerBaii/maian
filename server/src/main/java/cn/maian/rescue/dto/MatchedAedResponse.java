package cn.maian.rescue.dto;

import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;
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
    String ownerPhone,
    String vehicleInfo,
    int distanceMeters,
    int estimatedArrivalSeconds,
    String strategy,
    Instant matchedAt
) {
    public static MatchedAedResponse from(RescueCall rescueCall) {
        EmergencyDevice device = rescueCall.getMatchedDevice();
        if (device == null) {
            return null;
        }
        return new MatchedAedResponse(
            device.getId(),
            device.getType(),
            device.getName(),
            device.getCategory(),
            device.getAddress(),
            device.getLongitude(),
            device.getLatitude(),
            device.getOwnerPhone(),
            device.getVehicleInfo(),
            rescueCall.getMatchedDistanceMeters(),
            rescueCall.getEstimatedArrivalSeconds(),
            rescueCall.getMatchStrategy(),
            rescueCall.getMatchedAt()
        );
    }
}
