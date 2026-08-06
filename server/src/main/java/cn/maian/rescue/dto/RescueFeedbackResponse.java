package cn.maian.rescue.dto;

import cn.maian.rescue.domain.RescueFeedback;

import java.time.Instant;
import java.util.UUID;

public record RescueFeedbackResponse(
    UUID rescueCallId,
    UUID fromUserId,
    UUID toUserId,
    int rating,
    String comment,
    Instant createdAt
) {
    public static RescueFeedbackResponse from(RescueFeedback feedback) {
        return new RescueFeedbackResponse(
            feedback.getRescueCallId(),
            feedback.getFromUserId(),
            feedback.getToUserId(),
            feedback.getRating(),
            feedback.getComment(),
            feedback.getCreatedAt()
        );
    }
}
