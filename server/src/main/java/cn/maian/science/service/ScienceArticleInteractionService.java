package cn.maian.science.service;

import cn.maian.science.domain.ScienceArticleInteraction;
import cn.maian.science.dto.ScienceArticleInteractionResponse;
import cn.maian.science.dto.UpdateScienceArticleInteractionRequest;
import cn.maian.science.repository.ScienceArticleInteractionRepository;
import cn.maian.science.repository.ScienceContentRepository;
import cn.maian.user.service.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScienceArticleInteractionService {

    private static final int MAX_ARTICLE_ID_LENGTH = 80;

    private final ScienceArticleInteractionRepository interactionRepository;
    private final ScienceContentRepository contentRepository;
    private final CurrentUserService currentUserService;

    public ScienceArticleInteractionService(
        ScienceArticleInteractionRepository interactionRepository,
        ScienceContentRepository contentRepository,
        CurrentUserService currentUserService
    ) {
        this.interactionRepository = interactionRepository;
        this.contentRepository = contentRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public ScienceArticleInteractionResponse get(String articleId) {
        String normalizedArticleId = normalize(articleId);
        ensureArticleExists(normalizedArticleId);
        return interactionRepository
            .findByArticleIdAndUserId(normalizedArticleId, currentUserService.currentUserId())
            .map(ScienceArticleInteractionResponse::from)
            .orElseGet(() -> ScienceArticleInteractionResponse.empty(normalizedArticleId));
    }

    @Transactional
    public ScienceArticleInteractionResponse update(
        String articleId,
        UpdateScienceArticleInteractionRequest request
    ) {
        String normalizedArticleId = normalize(articleId);
        var content = contentRepository.findById(normalizedArticleId)
            .filter(item -> item.isActive() && item.getKind() == cn.maian.science.domain.ScienceContentKind.ARTICLE)
            .orElseThrow(() -> new IllegalArgumentException("文章不存在或已下架"));
        var interaction = interactionRepository
            .findByArticleIdAndUserId(normalizedArticleId, currentUserService.currentUserId())
            .orElseGet(() -> ScienceArticleInteraction.create(
                currentUserService.currentUserId(),
                normalizedArticleId
            ));
        boolean previousLiked = interaction.isLiked();
        boolean previousCollected = interaction.isCollected();
        interaction.update(request.liked(), request.collected());
        content.adjustInteractionCounts(
            Boolean.compare(request.liked(), previousLiked),
            Boolean.compare(request.collected(), previousCollected)
        );
        contentRepository.save(content);
        return ScienceArticleInteractionResponse.from(interactionRepository.save(interaction));
    }

    private void ensureArticleExists(String articleId) {
        boolean exists = contentRepository.findById(articleId)
            .filter(item -> item.isActive() && item.getKind() == cn.maian.science.domain.ScienceContentKind.ARTICLE)
            .isPresent();
        if (!exists) {
            throw new IllegalArgumentException("文章不存在或已下架");
        }
    }

    private String normalize(String articleId) {
        String normalized = articleId == null ? "" : articleId.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_ARTICLE_ID_LENGTH) {
            throw new IllegalArgumentException("文章编号格式错误");
        }
        return normalized;
    }
}
