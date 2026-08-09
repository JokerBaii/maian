package cn.maian.science.service;

import cn.maian.science.dto.UpdateScienceArticleInteractionRequest;
import cn.maian.science.repository.ScienceArticleInteractionRepository;
import cn.maian.science.repository.ScienceContentRepository;
import cn.maian.science.domain.ScienceContent;
import cn.maian.science.domain.ScienceContentKind;
import cn.maian.user.service.CurrentUserService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class ScienceArticleInteractionServiceTest {

    @Test
    void shouldPersistInteractionForApprovedSubmissionId() {
        var repository = mock(ScienceArticleInteractionRepository.class);
        var contentRepository = mock(ScienceContentRepository.class);
        var content = mock(ScienceContent.class);
        var currentUser = mock(CurrentUserService.class);
        var userId = UUID.randomUUID();
        var articleId = "submission-80000000-0000-0000-0000-000000000002";

        when(currentUser.currentUserId()).thenReturn(userId);
        when(repository.findByArticleIdAndUserId(articleId, userId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contentRepository.findById(articleId)).thenReturn(Optional.of(content));
        when(content.isActive()).thenReturn(true);
        when(content.getKind()).thenReturn(ScienceContentKind.ARTICLE);

        var response = new ScienceArticleInteractionService(repository, contentRepository, currentUser)
            .update(articleId, new UpdateScienceArticleInteractionRequest(true, true));

        assertThat(response.articleId()).isEqualTo(articleId);
        assertThat(response.liked()).isTrue();
        assertThat(response.collected()).isTrue();
        verify(content).adjustInteractionCounts(1, 1);
    }

    @Test
    void shouldRejectUnboundedArticleId() {
        var service = new ScienceArticleInteractionService(
            mock(ScienceArticleInteractionRepository.class),
            mock(ScienceContentRepository.class),
            mock(CurrentUserService.class)
        );

        assertThatThrownBy(() -> service.get("x".repeat(81)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("文章编号格式错误");
    }
}
