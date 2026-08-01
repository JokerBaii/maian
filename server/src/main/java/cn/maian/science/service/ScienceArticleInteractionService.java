package cn.maian.science.service;

import cn.maian.science.domain.ScienceArticleInteraction;
import cn.maian.science.dto.ScienceArticleInteractionResponse;
import cn.maian.science.dto.UpdateScienceArticleInteractionRequest;
import cn.maian.science.repository.ScienceArticleInteractionRepository;
import cn.maian.user.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScienceArticleInteractionService {

    private final ScienceArticleInteractionRepository interactionRepository;

    public ScienceArticleInteractionService(
        ScienceArticleInteractionRepository interactionRepository
    ) {
        this.interactionRepository = interactionRepository;
    }

    @Transactional(readOnly = true)
    public ScienceArticleInteractionResponse get(String articleId) {
        String normalizedArticleId = normalize(articleId);
        return interactionRepository
            .findByArticleIdAndUserId(normalizedArticleId, UserProfileService.CURRENT_USER_ID)
            .map(ScienceArticleInteractionResponse::from)
            .orElseGet(() -> ScienceArticleInteractionResponse.empty(normalizedArticleId));
    }

    @Transactional
    public ScienceArticleInteractionResponse update(
        String articleId,
        UpdateScienceArticleInteractionRequest request
    ) {
        String normalizedArticleId = normalize(articleId);
        var interaction = interactionRepository
            .findByArticleIdAndUserId(normalizedArticleId, UserProfileService.CURRENT_USER_ID)
            .orElseGet(() -> ScienceArticleInteraction.create(
                UserProfileService.CURRENT_USER_ID,
                normalizedArticleId
            ));
        interaction.update(request.liked(), request.collected());
        return ScienceArticleInteractionResponse.from(interactionRepository.save(interaction));
    }

    private String normalize(String articleId) {
        String normalized = articleId == null ? "" : articleId.trim();
        if (normalized.isEmpty() || normalized.length() > 40) {
            throw new IllegalArgumentException("文章编号格式错误");
        }
        return normalized;
    }
}
