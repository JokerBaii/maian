package cn.maian.science.repository;

import cn.maian.science.domain.ScienceArticleInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScienceArticleInteractionRepository
    extends JpaRepository<ScienceArticleInteraction, UUID> {

    Optional<ScienceArticleInteraction> findByArticleIdAndUserId(String articleId, UUID userId);
}
