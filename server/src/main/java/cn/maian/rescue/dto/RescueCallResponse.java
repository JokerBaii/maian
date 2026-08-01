package cn.maian.rescue.dto;

import cn.maian.rescue.domain.RescueCall;
import cn.maian.rescue.domain.RescueStatus;
import cn.maian.rescue.domain.UrgencyLevel;

import java.time.Instant;
import java.util.Set;
import java.util.List;
import java.util.UUID;

public record RescueCallResponse(
    UUID id,
    UrgencyLevel urgency,
    RescueStatus status,
    double latitude,
    double longitude,
    String address,
    String description,
    Set<String> symptoms,
    List<String> imageUrls,
    MatchedAedResponse matchedAed,
    Instant createdAt,
    Instant updatedAt
) {
    public static RescueCallResponse from(RescueCall rescueCall) {
        return new RescueCallResponse(
            rescueCall.getId(),
            rescueCall.getUrgency(),
            rescueCall.getStatus(),
            rescueCall.getLatitude(),
            rescueCall.getLongitude(),
            rescueCall.getAddress(),
            rescueCall.getDescription(),
            rescueCall.getSymptoms(),
            rescueCall.getImageUrls(),
            MatchedAedResponse.from(rescueCall),
            rescueCall.getCreatedAt(),
            rescueCall.getUpdatedAt()
        );
    }
}
