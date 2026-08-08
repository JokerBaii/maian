package cn.maian.science.dto;

import cn.maian.science.domain.ScienceSubmission;
import cn.maian.science.domain.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record ScienceSubmissionResponse(
    UUID id,
    String title,
    String category,
    String content,
    SubmissionStatus status,
    boolean hasCoverImage,
    UUID coverMediaId,
    Instant submittedAt,
    String reviewNote,
    Instant reviewedAt
) {
    public static ScienceSubmissionResponse from(ScienceSubmission submission) {
        return new ScienceSubmissionResponse(
            submission.getId(),
            submission.getTitle(),
            submission.getCategory(),
            submission.getContent(),
            submission.getStatus(),
            submission.isHasCoverImage(),
            submission.getCoverMediaId(),
            submission.getSubmittedAt(),
            submission.getReviewNote(),
            submission.getReviewedAt()
        );
    }
}
