package cn.maian.rescue.dto;

import cn.maian.rescue.domain.RescueCall;
import cn.maian.device.domain.DeviceType;

import java.time.Instant;

public record LiveTrackingResponse(
    Double responderLatitude,
    Double responderLongitude,
    Instant reportedAt,
    String source
) {
    public static LiveTrackingResponse from(RescueCall rescueCall) {
        var device = rescueCall.getMatchedDevice();
        boolean useMobileDevice = device != null
            && device.getType() == DeviceType.MOBILE
            && device.getLastLocationAt() != null
            && (rescueCall.getResponderLocationAt() == null
                || device.getLastLocationAt().isAfter(rescueCall.getResponderLocationAt()));
        if (useMobileDevice) {
            return new LiveTrackingResponse(
                device.getLatitude(), device.getLongitude(), device.getLastLocationAt(), "MOBILE_AED"
            );
        }
        if (rescueCall.getResponderLocationAt() == null) {
            return null;
        }
        return new LiveTrackingResponse(
            rescueCall.getResponderLatitude(),
            rescueCall.getResponderLongitude(),
            rescueCall.getResponderLocationAt(),
            "RESPONDER"
        );
    }
}
