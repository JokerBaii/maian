package cn.maian.rescue.dto;

import cn.maian.rescue.domain.AedCustodyStatus;
import cn.maian.rescue.domain.RescueCall;
import cn.maian.rescue.domain.RescueStatus;
import cn.maian.rescue.domain.UrgencyLevel;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record RequesterRescueResponse(
    UUID id,
    UrgencyLevel urgency,
    RescueStatus status,
    double latitude,
    double longitude,
    String address,
    String description,
    Set<String> symptoms,
    List<UUID> attachmentMediaIds,
    MatchedAedResponse matchedAed,
    AedCustodyStatus aedCustodyStatus,
    UUID responderUserId,
    RescueParticipantResponse responder,
    LiveTrackingResponse liveTracking,
    Instant matchDeadlineAt,
    Instant acceptedAt,
    Instant arrivedAtAedAt,
    Instant arrivedAt,
    Instant rescueStartedAt,
    Instant completionSubmittedAt,
    Instant confirmationDeadlineAt,
    Instant completedAt,
    Instant aedReturnedAt,
    long eventSequence,
    Instant createdAt,
    Instant updatedAt
) {
    public static RequesterRescueResponse from(RescueCall rescueCall) {
        return from(rescueCall, null);
    }

    public static RequesterRescueResponse from(
        RescueCall rescueCall,
        RescueParticipantResponse responder
    ) {
        return new RequesterRescueResponse(
            rescueCall.getId(), rescueCall.getUrgency(), rescueCall.getStatus(),
            rescueCall.getLatitude(), rescueCall.getLongitude(), rescueCall.getAddress(),
            rescueCall.getDescription(), rescueCall.getSymptoms(), rescueCall.getAttachmentMediaIds(),
            MatchedAedResponse.from(rescueCall, rescueCall.getResponderUserId() != null),
            rescueCall.getAedCustodyStatus(),
            rescueCall.getResponderUserId(), responder, LiveTrackingResponse.from(rescueCall),
            rescueCall.getMatchDeadlineAt(), rescueCall.getAcceptedAt(), rescueCall.getArrivedAtAedAt(),
            rescueCall.getArrivedAt(),
            rescueCall.getRescueStartedAt(), rescueCall.getCompletionSubmittedAt(),
            rescueCall.getConfirmationDeadlineAt(), rescueCall.getCompletedAt(),
            rescueCall.getAedReturnedAt(), rescueCall.getEventSequence(),
            rescueCall.getCreatedAt(), rescueCall.getUpdatedAt()
        );
    }
}
