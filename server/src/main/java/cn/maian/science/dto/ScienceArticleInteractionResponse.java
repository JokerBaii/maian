package cn.maian.science.dto;

import cn.maian.science.domain.ScienceArticleInteraction;

import java.time.Instant;

public record ScienceArticleInteractionResponse(
    String articleId,
    boolean liked,
    boolean collected,
    Instant updatedAt
) {
    public static ScienceArticleInteractionResponse empty(String articleId) {
        return new ScienceArticleInteractionResponse(articleId, false, false, null);
    }

    public static ScienceArticleInteractionResponse from(ScienceArticleInteraction interaction) {
        return new ScienceArticleInteractionResponse(
            interaction.getArticleId(),
            interaction.isLiked(),
            interaction.isCollected(),
            interaction.getUpdatedAt()
        );
    }
}
