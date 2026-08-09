package cn.maian.science.dto;

import cn.maian.science.domain.ScienceContent;
import cn.maian.science.domain.ScienceContentKind;

import java.time.Instant;
import java.util.UUID;

public record ScienceContentResponse(
    String id,
    ScienceContentKind kind,
    String category,
    String title,
    String summary,
    String body,
    String author,
    String coverPath,
    UUID coverMediaId,
    String mediaUrl,
    String sourceName,
    String sourcePage,
    String durationLabel,
    int viewCount,
    int likeCount,
    int collectCount,
    Instant publishedAt
) {
    public static ScienceContentResponse from(ScienceContent content) {
        return new ScienceContentResponse(
            content.getId(), content.getKind(), content.getCategory(), content.getTitle(),
            content.getSummary(), content.getBody(), content.getAuthor(), content.getCoverPath(),
            content.getCoverMediaId(), content.getMediaUrl(), content.getSourceName(),
            content.getSourcePage(), content.getDurationLabel(), content.getViewCount(),
            content.getLikeCount(), content.getCollectCount(), content.getPublishedAt()
        );
    }
}
