package cn.maian.rescue.dto;

import cn.maian.device.domain.DeviceType;
import cn.maian.rescue.domain.AedCustodyStatus;
import cn.maian.rescue.domain.RescueCall;
import cn.maian.rescue.domain.RescueStatus;
import cn.maian.rescue.domain.UrgencyLevel;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ResponderTaskResponse(
    UUID id,
    UrgencyLevel urgency,
    RescueStatus status,
    boolean detailAvailable,
    Integer distanceToRequesterMeters,
    Double latitude,
    Double longitude,
    String address,
    String description,
    Set<String> symptoms,
    List<UUID> attachmentMediaIds,
    MatchedAedResponse matchedAed,
    DeviceType matchedAedType,
    AedCustodyStatus aedCustodyStatus,
    Instant acceptedAt,
    Instant arrivedAtAedAt,
    Instant completionSubmittedAt,
    Instant confirmationDeadlineAt,
    long eventSequence,
    Instant createdAt,
    Instant updatedAt
) {
    public static ResponderTaskResponse offer(RescueCall call, int distanceMeters) {
        return new ResponderTaskResponse(
            call.getId(), call.getUrgency(), call.getStatus(), false, distanceMeters,
            null, null, null, null, Set.of(), List.of(), null,
            call.getMatchedDevice() == null ? null : call.getMatchedDevice().getType(),
            call.getAedCustodyStatus(), null, null, null, null,
            call.getEventSequence(), call.getCreatedAt(), call.getUpdatedAt()
        );
    }

    public static ResponderTaskResponse assigned(RescueCall call) {
        return new ResponderTaskResponse(
            call.getId(), call.getUrgency(), call.getStatus(), true, null,
            call.getLatitude(), call.getLongitude(), call.getAddress(), call.getDescription(),
            call.getSymptoms(), call.getAttachmentMediaIds(), MatchedAedResponse.from(call, true),
            call.getMatchedDevice() == null ? null : call.getMatchedDevice().getType(),
            call.getAedCustodyStatus(), call.getAcceptedAt(), call.getArrivedAtAedAt(),
            call.getCompletionSubmittedAt(),
            call.getConfirmationDeadlineAt(), call.getEventSequence(), call.getCreatedAt(), call.getUpdatedAt()
        );
    }
}
