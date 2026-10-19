package cn.maian.agent.service;

import cn.maian.science.domain.ScienceContent;
import cn.maian.science.repository.ScienceContentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssistantServiceTest {
    private final ScienceContentRepository contents = mock(ScienceContentRepository.class);
    private final AgentModelGateway model = mock(AgentModelGateway.class);
    private final AssistantService service = new AssistantService(contents, model);

    @Test
    void locatesSettingsSectionWithoutModelOrChangingSettings() {
        var result = service.assist("怎么关闭健康预警通知", false);
        assertThat(result.actions()).singleElement().satisfies(action ->
            assertThat(action.url()).isEqualTo("/pages/mine/settings?section=notifications"));
        assertThat(result.executionMode()).isEqualTo("RULES");
        verifyNoInteractions(model, contents);
    }

    @Test
    void findsCoordinationProgressWithoutRequestingMedicalAdvice() {
        var result = service.assist("设备故障处理进度在哪里看", false);
        assertThat(result.actions()).singleElement().satisfies(action ->
            assertThat(action.url()).isEqualTo("/pages/agent/index"));
        verifyNoInteractions(model, contents);
    }

    @Test
    void fallsBackWhenModelTimesOutAndOnlyUsesRealPublishedArticleIds() {
        when(model.invoke(anyString(), anyString(), any())).thenReturn("RULE_FALLBACK");
        var article = mock(ScienceContent.class);
        when(article.getId()).thenReturn("submission-verified");
        when(article.getTitle()).thenReturn("AED 使用知识");
        when(article.getSummary()).thenReturn("已审核的文章摘要");
        when(contents.searchPublishedArticles(eq("aed"), any(Pageable.class))).thenReturn(List.of(article));
        var result = service.assist("推荐 AED 文章", true);
        assertThat(result.executionMode()).isEqualTo("RULE_FALLBACK");
        assertThat(result.actions()).singleElement().satisfies(action -> {
            assertThat(action.kind()).isEqualTo("ARTICLE");
            assertThat(action.url()).isEqualTo("/pages/science/detail?id=submission-verified");
        });
    }

    @Test
    void emptyCatalogDoesNotInventArticleOrSearchUnrelatedContent() {
        when(contents.searchPublishedArticles(anyString(), any(Pageable.class))).thenReturn(List.of());
        assertThat(service.assist("推荐心肺复苏文章", false).actions()).isEmpty();
        verify(contents).searchPublishedArticles(eq("心肺复苏"), any(Pageable.class));
        clearInvocations(contents);
        assertThat(service.assist("帮我诊断", false).actions()).isEmpty();
        verifyNoInteractions(contents);
    }
}
