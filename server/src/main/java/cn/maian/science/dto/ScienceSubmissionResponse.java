package cn.maian.science.dto;

import cn.maian.science.domain.ScienceSubmission;
import cn.maian.science.domain.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record ScienceSubmissionResponse(
    UUID id,
    String title,
    String category,
    SubmissionStatus status,
    boolean hasCoverImage,
    String coverImageUrl,
    Instant submittedAt
) {
    public static ScienceSubmissionResponse from(ScienceSubmission submission) {
        return new ScienceSubmissionResponse(
            submission.getId(),
            submission.getTitle(),
            submission.getCategory(),
            submission.getStatus(),
            submission.isHasCoverImage(),
            submission.getCoverImageUrl(),
            submission.getSubmittedAt()
        );
    }
}
