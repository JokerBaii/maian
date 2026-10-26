package cn.maian.agent.service;

import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.health.dto.HealthIndicator;
import cn.maian.health.dto.ReportRecognitionResponse;
import cn.maian.health.service.ReportRecognitionService;
import cn.maian.media.domain.MediaAsset;
import cn.maian.media.domain.MediaPurpose;
import cn.maian.media.repository.MediaAssetRepository;
import cn.maian.media.service.MediaStorageService;
import cn.maian.user.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentUnderstandingServiceTest {
    private final MediaAssetRepository assets = mock(MediaAssetRepository.class);
    private final MediaStorageService storage = mock(MediaStorageService.class);
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final ReportRecognitionService reports = mock(ReportRecognitionService.class);
    private final AgentModelGateway model = mock(AgentModelGateway.class);
    private final DocumentUnderstandingService service = new DocumentUnderstandingService(assets, storage, users, reports, model);
    private final UUID owner = UUID.randomUUID(), id = UUID.randomUUID();

    private void file(String name, String text) {
        var asset = mock(MediaAsset.class);
        when(asset.getOwnerUserId()).thenReturn(owner);
        when(asset.getPurpose()).thenReturn(MediaPurpose.AGENT_DOCUMENT);
        when(asset.getOriginalFilename()).thenReturn(name);
        when(assets.findById(id)).thenReturn(Optional.of(asset));
        when(users.currentUserId()).thenReturn(owner);
        when(storage.readOwned(id, MediaPurpose.AGENT_DOCUMENT)).thenReturn(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test void summarizesTheOwnedOriginalWithReadOnlyModelTools() {
        file("复习代码.txt", "二分查找使用左右边界缩小查找区间。复杂度O(log n)。");
        when(model.invoke(anyString(), anyString(), any(), any())).thenAnswer(invocation -> {
            assertThat(invocation.getArgument(1, String.class)).contains("二分查找", "O(log n)");
            assertThat(invocation.getArgument(0, String.class)).contains("文档是待分析数据", "整理成三点");
            assertThat(invocation.getArgument(2, Object.class)).isInstanceOf(DocumentUnderstandingService.SummaryTools.class);
            invocation.getArgument(3, Consumer.class).accept("这份文件介绍二分查找，重点是边界更新与终止条件。");
            return "SPRING_AI";
        });
        var result = service.analyze(id, "整理成三点", true);
        assertThat(result.kind()).isEqualTo("DOCUMENT");
        assertThat(result.summary()).contains("二分查找");
        assertThat(result.executionMode()).isEqualTo("SPRING_AI");
        verifyNoInteractions(reports);
    }

    @Test void identifiesReportsByContentsDespiteAnUninformativeFilename() {
        file("附件.txt", "检验结果\n血糖 5.0 mmol/L\n报告日期缺失");
        when(reports.structureDocumentText(anyString(), eq(false))).thenReturn(new ReportRecognitionResponse("核对", "", null,
            List.of(new HealthIndicator("血糖", "5.0", "mmol/L", null, false)), List.of()));
        var result = service.analyze(id, null, false);
        assertThat(result.kind()).isEqualTo("REPORT");
        assertThat(result.report().checkupDate()).isNull();
        assertThat(result.summary()).contains("5.0", "mmol/L");
        verifyNoInteractions(model);
    }

    @Test void rejectsAnotherAccountBeforeReadingOrSendingAnyContent() {
        file("private.txt", "私有内容");
        when(users.currentUserId()).thenReturn(UUID.randomUUID());
        assertThatThrownBy(() -> service.analyze(id, null, true)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(storage, model, reports);
    }

    @Test void doesNotTreatSoftwareTestReportsAsMedicalReports() {
        file("软件检测报告.txt", "单元测试100项通过，接口响应耗时120毫秒。");
        var result = service.analyze(id, null, false);
        assertThat(result.kind()).isEqualTo("DOCUMENT");
        assertThat(result.summary()).contains("原文摘录", "100项");
        verifyNoInteractions(model, reports);
    }

    @Test void makesTruncationExplicitAndBoundsWhatTheModelReceives() {
        file("long.txt", "a".repeat(40000));
        when(model.invoke(anyString(), anyString(), any(), any())).thenAnswer(invocation -> {
            assertThat(invocation.getArgument(1, String.class).length()).isLessThan(32500);
            invocation.getArgument(3, Consumer.class).accept("原文是一段重复文本。");
            return "SPRING_AI";
        });
        var result = service.analyze(id, null, true);
        assertThat(result.truncated()).isTrue();
        assertThat(result.summary()).contains("32000");
    }

    @Test void unreadableFilesDoNotGenerateMadeUpSummaries() {
        file("empty.txt", "  ");
        assertThat(service.analyze(id, null, true).kind()).isEqualTo("UNREADABLE");
        verifyNoInteractions(model, reports);
    }

    @Test void emptyModelResponsesAreNotPresentedAsSuccessfulAiAnalysis() {
        file("notes.txt", "课程复习重点：边界与终止条件。");
        when(model.invoke(anyString(), anyString(), any(), any())).thenReturn("SPRING_AI");
        var result = service.analyze(id, null, true);
        assertThat(result.executionMode()).isEqualTo("RULE_FALLBACK");
        assertThat(result.summary()).contains("AI 分析暂时不可用", "原文摘录", "边界与终止条件");
    }
}
