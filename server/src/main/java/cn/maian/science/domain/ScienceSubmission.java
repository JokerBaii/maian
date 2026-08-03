package cn.maian.science.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "science_submissions")
public class ScienceSubmission {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID userId;

    @Column(length = 50, nullable = false)
    private String title;

    @Column(length = 30, nullable = false)
    private String category;

    @Column(length = 2000, nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean hasCoverImage;

    @Column(length = 500)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SubmissionStatus status;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(length = 300)
    private String reviewNote;

    private Instant reviewedAt;

    protected ScienceSubmission() {
    }

    public static ScienceSubmission create(
        UUID userId,
        String title,
        String category,
        String content,
        String coverImageUrl
    ) {
        var submission = new ScienceSubmission();
        submission.id = UUID.randomUUID();
        submission.userId = userId;
        submission.title = title;
        submission.category = category;
        submission.content = content;
        submission.coverImageUrl = coverImageUrl;
        submission.hasCoverImage = coverImageUrl != null && !coverImageUrl.isBlank();
        submission.status = SubmissionStatus.PENDING;
        submission.submittedAt = Instant.now();
        return submission;
    }

    public void review(boolean approved, String reviewNote) {
        if (status != SubmissionStatus.PENDING) {
            throw new IllegalStateException("投稿已经完成审核");
        }
        status = approved ? SubmissionStatus.APPROVED : SubmissionStatus.REJECTED;
        this.reviewNote = reviewNote == null || reviewNote.isBlank() ? null : reviewNote.trim();
        reviewedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getContent() {
        return content;
    }

    public UUID getUserId() {
        return userId;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public boolean isHasCoverImage() {
        return hasCoverImage;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
