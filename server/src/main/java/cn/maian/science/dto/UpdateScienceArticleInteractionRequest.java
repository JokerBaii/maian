package cn.maian.science.dto;

public record UpdateScienceArticleInteractionRequest(
    boolean liked,
    boolean collected
) {
}
