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
@Table(name = "science_contents")
public class ScienceContent {

    @Id
    @Column(length = 80, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ScienceContentKind kind;

    @Column(length = 30)
    private String category;

    @Column(length = 80, nullable = false)
    private String title;

    @Column(length = 500, nullable = false)
    private String summary;

    @Column(columnDefinition = "LONGTEXT")
    private String body;

    @Column(length = 80, nullable = false)
    private String author;

    @Column(length = 255)
    private String coverPath;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID coverMediaId;

    @Column(length = 1000)
    private String mediaUrl;

    @Column(length = 120)
    private String sourceName;

    @Column(length = 1000)
    private String sourcePage;

    @Column(length = 40)
    private String durationLabel;

    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private int collectCount;

    @Column(nullable = false)
    private Instant publishedAt;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, unique = true)
    private UUID sourceSubmissionId;

    protected ScienceContent() {
    }

    public static ScienceContent fromApprovedSubmission(ScienceSubmission submission) {
        var content = new ScienceContent();
        content.id = "submission-" + submission.getId();
        content.kind = ScienceContentKind.ARTICLE;
        content.category = submission.getCategory();
        content.title = submission.getTitle();
        content.summary = summarize(submission.getContent());
        content.body = submission.getContent();
        content.author = "用户投稿";
        content.coverMediaId = submission.getCoverMediaId();
        content.viewCount = 0;
        content.likeCount = 0;
        content.collectCount = 0;
        content.publishedAt = submission.getReviewedAt() == null
            ? submission.getSubmittedAt()
            : submission.getReviewedAt();
        content.sortOrder = 500;
        content.active = true;
        content.sourceSubmissionId = submission.getId();
        return content;
    }

    private static String summarize(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 100 ? normalized : normalized.substring(0, 100) + "…";
    }

    public void adjustInteractionCounts(int likeDelta, int collectDelta) {
        likeCount = Math.max(0, likeCount + likeDelta);
        collectCount = Math.max(0, collectCount + collectDelta);
    }

    public String getId() { return id; }
    public ScienceContentKind getKind() { return kind; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getBody() { return body; }
    public String getAuthor() { return author; }
    public String getCoverPath() { return coverPath; }
    public UUID getCoverMediaId() { return coverMediaId; }
    public String getMediaUrl() { return mediaUrl; }
    public String getSourceName() { return sourceName; }
    public String getSourcePage() { return sourcePage; }
    public String getDurationLabel() { return durationLabel; }
    public int getViewCount() { return viewCount; }
    public int getLikeCount() { return likeCount; }
    public int getCollectCount() { return collectCount; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }
    public UUID getSourceSubmissionId() { return sourceSubmissionId; }
}
