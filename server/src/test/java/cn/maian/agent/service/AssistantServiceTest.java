package cn.maian.agent.service;

import cn.maian.agent.dto.AssistantContext;
import cn.maian.science.domain.ScienceContent;
import cn.maian.science.repository.ScienceContentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        when(model.invoke(anyString(), anyString(), any(), any())).thenReturn("RULE_FALLBACK");
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

    @Test
    void compoundRequestsReachTheModelWithTheEntireInstruction() {
        String message = "开始接单，并关闭科普更新";
        when(model.invoke(anyString(), eq(message), any(), any())).thenAnswer(invocation -> {
            var tools = invocation.getArgument(2, AssistantService.AssistantTools.class);
            tools.planTask("VOLUNTEER_AVAILABILITY", Map.of("available", "true"));
            tools.planTask("SETTINGS_UPDATE", Map.of("scienceUpdate", "false"));
            return "SPRING_AI";
        });
        var result = service.assist(message, true);
        assertThat(result.executionMode()).isEqualTo("SPRING_AI");
        assertThat(result.tasks()).extracting(task -> task.operation())
            .containsExactly("VOLUNTEER_AVAILABILITY", "SETTINGS_UPDATE");
        verify(model).invoke(anyString(), eq(message), any(), any());
        verifyNoInteractions(contents);
    }

    @Test
    void modelFailurePreservesEveryRecognizedCompoundActionInOrder() {
        when(model.invoke(anyString(), anyString(), any(), any())).thenReturn("RULE_FALLBACK");
        var result = service.assist("暂停接单，然后查看我的救援", true);
        assertThat(result.executionMode()).isEqualTo("RULE_FALLBACK");
        assertThat(result.tasks()).extracting(task -> task.operation())
            .containsExactly("VOLUNTEER_AVAILABILITY", "RESCUE_LIST");
        assertThat(result.tasks().getFirst().parameters()).containsEntry("available", "false");
        verify(model).invoke(anyString(), eq("暂停接单，然后查看我的救援"), any(), any());
    }

    @Test
    void reminderAndContextualRescueCommandsDoNotSwallowTheirFollowingActions() {
        when(model.invoke(anyString(), anyString(), any(), any())).thenReturn("RULE_FALLBACK");
        String reminder = "十分钟后提醒我归还AED，顺便关闭科普更新";
        var reminders = service.assist(reminder, true);
        assertThat(reminders.tasks()).extracting(task -> task.operation())
            .containsExactly("REMINDER_CREATE", "SETTINGS_UPDATE");
        assertThat(reminders.tasks().getFirst().parameters().get("title")).contains("AED").doesNotContain("关闭科普更新");
        var context = new AssistantContext("pages/rescue/detail", null, UUID.randomUUID().toString(), null, null, null, null, null, null, null);
        String rescue = "确认归还AED，然后生成救援复盘";
        assertThat(service.assist(rescue, true, context, null).tasks()).extracting(task -> task.operation())
            .containsExactly("RESCUE_RETURN", "RESCUE_REVIEW");
        verify(model).invoke(anyString(), eq(reminder), any(), any());
        verify(model).invoke(anyString(), eq(rescue), any(), any());
    }

    @Test
    void singleActionCommandsStillBypassModelLatency() {
        assertThat(service.assist("开始接单", true).tasks()).singleElement()
            .satisfies(task -> assertThat(task.operation()).isEqualTo("VOLUNTEER_AVAILABILITY"));
        assertThat(service.assist("十分钟后提醒我归还AED", true).tasks()).singleElement()
            .satisfies(task -> assertThat(task.operation()).isEqualTo("REMINDER_CREATE"));
        verifyNoInteractions(model, contents);
    }

    @Test
    void localFallbackDoesNotPartiallyExecuteAmbiguousOrOversizedCompoundRequests() {
        assertThat(AssistantOperations.local("开始接单，然后帮我跳舞")).isEmpty();
        assertThat(AssistantOperations.local("开始接单，并不要关闭科普更新")).isEmpty();
        assertThat(AssistantOperations.local("开始接单\n不要关闭科普更新")).isEmpty();
        assertThat(AssistantOperations.local("查看设置；查看设备；查看联系人；查看报告；查看救援")).isEmpty();
        var pending = new AssistantContext.Pending("SCIENCE_SUBMIT", List.of("title"), List.of("title"));
        assertThat(service.assist("标题改成新标题，然后帮我跳舞", false, null, pending).continuation()).isFalse();
        assertThat(AssistantContinuation.local("标题改成新标题，然后帮我跳舞", pending)).isEmpty();
    }
}
