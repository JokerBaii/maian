package cn.maian.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import cn.maian.science.repository.ScienceContentRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Exercises Spring AI's HTTP protocol, @Tool registration, tool-result round trips and gateway. */
class SpringAiAgentIntegrationTest {
    private final ObjectMapper json = new ObjectMapper();
    private MockRestServiceServer http;
    private AgentModelGateway gateway;

    @BeforeEach
    void setUp() {
        var rest = RestClient.builder();
        http = MockRestServiceServer.bindTo(rest).build();
        var api = OpenAiApi.builder().baseUrl("http://model.test").apiKey("integration-test")
            .restClientBuilder(rest).build();
        var model = OpenAiChatModel.builder().openAiApi(api)
            .defaultOptions(OpenAiChatOptions.builder().model("tool-contract").build())
            .retryTemplate(RetryTemplate.builder().maxAttempts(1).build()).build();
        var beans = new StaticListableBeanFactory();
        beans.addBean("chatClientBuilder", ChatClient.builder(model));
        gateway = new AgentModelGateway(true, beans.getBeanProvider(ChatClient.Builder.class));
    }

    @AfterEach
    void tearDown() { gateway.shutdown(); }

    @Test
    void modelToolsDriveResourceCoordinationInOrder() throws Exception {
        var service = mock(ResourceCoordinationService.class);
        UUID id = UUID.randomUUID();
        when(service.pending()).thenReturn(List.of(id));
        when(service.claim(id)).thenReturn(true);
        when(service.inspectDevice(id)).thenReturn("status=DISABLED; dispatchable=false");
        when(service.inspectImpact(id)).thenReturn("associated=true; accepted=false");
        when(service.findAlternatives(id)).thenReturn("candidate_count=1; estimated_arrival_seconds=[60]");
        when(service.coordinate(id)).thenReturn("已重新匹配可用 AED");
        expectTool("inspectDevice", Map.of());
        expectTool("inspectRescueImpact", Map.of());
        expectTool("findAlternatives", Map.of());
        expectTool("coordinateResources", Map.of());
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andExpect(jsonPath("$.messages[?(@.role == 'tool')]").value(org.hamcrest.Matchers.hasSize(4)))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("candidate_count=1")))
            .andRespond(withSuccess(completion("协同完成"), MediaType.APPLICATION_JSON));

        new ResourceCoordinationAgent(service, gateway).process();

        var order = inOrder(service);
        order.verify(service).inspectDevice(id);
        order.verify(service).inspectImpact(id);
        order.verify(service).findAlternatives(id);
        order.verify(service).coordinate(id);
        order.verify(service).complete(id, "SPRING_AI");
        verify(service, never()).fail(id);
        http.verify();
    }

    @Test
    void prematureModelExecutionFallsBackToOrderedBusinessWorkflow() throws Exception {
        var service = mock(ResourceCoordinationService.class);
        UUID id = UUID.randomUUID();
        when(service.pending()).thenReturn(List.of(id));
        when(service.claim(id)).thenReturn(true);
        expectTool("coordinateResources", Map.of());
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("请先完成设备、任务与候选资源检查")))
            .andRespond(withSuccess(completion("未能完成工具执行"), MediaType.APPLICATION_JSON));

        new ResourceCoordinationAgent(service, gateway).process();

        var order = inOrder(service);
        order.verify(service).inspectDevice(id);
        order.verify(service).inspectImpact(id);
        order.verify(service).findAlternatives(id);
        order.verify(service).coordinate(id);
        order.verify(service).complete(id, "RULE_FALLBACK");
        verify(service, times(1)).coordinate(id);
        http.verify();
    }

    @Test
    void modelPlansImmediateRescueAndKeepsTheObservedDescription() throws Exception {
        var contents = mock(ScienceContentRepository.class);
        expectTool("planTask", Map.of("operation", "RESCUE_CREATE", "parameters", Map.of("description", "有个人晕倒了，需要急救")));
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andRespond(withSuccess(completion("我马上打开紧急呼救页。"), MediaType.APPLICATION_JSON));
        var result = new AssistantService(contents, gateway).assist("有个人晕倒了，需要急救", true);
        assertThat(result.executionMode()).isEqualTo("SPRING_AI");
        assertThat(result.tasks()).hasSize(1);
        assertThat(result.tasks().getFirst().operation()).isEqualTo("RESCUE_CREATE");
        assertThat(result.tasks().getFirst().parameters()).containsEntry("description", "有个人晕倒了，需要急救");
        assertThat(result.tasks().getFirst().parameters()).doesNotContainKeys("latitude", "longitude", "urgency");
        verifyNoInteractions(contents);
        http.verify();
    }

    @Test
    void hypotheticalHistoricalAndNegatedRescuesDoNotTakeTheImmediateRoute() {
        for (String text : List.of("不要发起呼救", "别发起呼救", "如果有人晕倒了，需要急救", "昨天有人晕倒了", "学习有人晕倒时的急救知识", "如何发起呼救", "取消呼救")) {
            assertThat(AssistantOperations.isImmediateRescue(text)).as(text).isFalse();
        }
    }

    @Test
    void modelCanSupplementAnExistingTaskWithoutReplanningOrSendingItsOldValues() throws Exception {
        expectTool("continueTask", Map.of("parameters", Map.of("category","device")));
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andRespond(withSuccess(completion("补充完成"), MediaType.APPLICATION_JSON));
        var result = new AssistantService(mock(ScienceContentRepository.class), gateway).assist("归到设备知识",true,null,
            new cn.maian.agent.dto.AssistantContext.Pending("SCIENCE_SUBMIT",List.of("category"),List.of("category")));
        assertThat(result.executionMode()).isEqualTo("SPRING_AI");
        assertThat(result.continuation()).isTrue();
        assertThat(result.tasks()).hasSize(1);
        assertThat(result.tasks().getFirst().parameters()).containsExactlyEntriesOf(Map.of("category","device"));
        http.verify();
    }

    @Test
    void modelPlansAValidatedBusinessOperationWithoutInventingExecutionSuccess() throws Exception {
        expectTool("planTask", Map.of("operation", "SETTINGS_UPDATE", "parameters", Map.of("scienceUpdate", "false")));
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andRespond(withSuccess(completion("已完成设置修改"), MediaType.APPLICATION_JSON));
        var contents = mock(ScienceContentRepository.class);
        var result = new AssistantService(contents, gateway).assist("把科普推送关掉", true);
        assertThat(result.executionMode()).isEqualTo("SPRING_AI");
        assertThat(result.tasks()).singleElement().satisfies(task -> {
            assertThat(task.operation()).isEqualTo("SETTINGS_UPDATE");
            assertThat(task.parameters()).containsEntry("scienceUpdate", "false");
        });
        assertThat(result.reply()).doesNotContain("已完成设置修改");
        verifyNoInteractions(contents); http.verify();
    }

    @Test
    void compoundCommandUsesModelToolsForBothOperationsInOrder() throws Exception {
        expectTool("planTask", Map.of("operation", "VOLUNTEER_AVAILABILITY", "parameters", Map.of("available", "true")));
        expectTool("planTask", Map.of("operation", "SETTINGS_UPDATE", "parameters", Map.of("scienceUpdate", "false")));
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("开始接单，并关闭科普更新")))
            .andRespond(withSuccess(completion("准备按顺序办理"), MediaType.APPLICATION_JSON));
        var result = new AssistantService(mock(ScienceContentRepository.class), gateway)
            .assist("开始接单，并关闭科普更新", true);
        assertThat(result.executionMode()).isEqualTo("SPRING_AI");
        assertThat(result.tasks()).extracting(task -> task.operation())
            .containsExactly("VOLUNTEER_AVAILABILITY", "SETTINGS_UPDATE");
        assertThat(result.tasks().get(1).parameters()).containsEntry("scienceUpdate", "false");
        http.verify();
    }

    @Test
    void assistantUsesToolCardsAndIgnoresModelInventedResult() throws Exception {
        expectTool("findAppPage", Map.of("query", "资源协同"));
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/pages/agent/index")))
            .andRespond(withSuccess(completion("请打开 /pages/admin/accounts"), MediaType.APPLICATION_JSON));

        var contents = mock(ScienceContentRepository.class);
        var result = new AssistantService(contents, gateway).assist("设备故障处理进度在哪里看", true);

        assertThat(result.executionMode()).isEqualTo("SPRING_AI");
        assertThat(result.actions()).singleElement().satisfies(action ->
            assertThat(action.url()).isEqualTo("/pages/agent/index"));
        assertThat(result.reply()).doesNotContain("/pages/admin/accounts");
        verifyNoInteractions(contents);
        http.verify();
    }

    @Test
    void modelCanGreetNaturallyWithoutBeingForcedToCallASearchTool() throws Exception {
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andRespond(withSuccess(completion("Hello！我是脉脉，今天想聊什么？"), MediaType.APPLICATION_JSON));
        var contents = mock(ScienceContentRepository.class);
        var result = new AssistantService(contents, gateway).assist("Hello", true);
        assertThat(result.executionMode()).isEqualTo("SPRING_AI");
        assertThat(result.conversation()).isTrue();
        assertThat(result.reply()).isEqualTo("Hello！我是脉脉，今天想聊什么？");
        assertThat(result.tasks()).isEmpty(); assertThat(result.actions()).isEmpty(); assertThat(result.steps()).isEmpty();
        verifyNoInteractions(contents); http.verify();
    }

    @Test
    void documentReaderOnlyExposesTheReadOnlySummaryTool() throws Exception {
        var assets = mock(cn.maian.media.repository.MediaAssetRepository.class);
        var storage = mock(cn.maian.media.service.MediaStorageService.class);
        var users = mock(cn.maian.user.service.CurrentUserService.class);
        var reports = mock(cn.maian.health.service.ReportRecognitionService.class);
        var asset = mock(cn.maian.media.domain.MediaAsset.class);
        UUID owner = UUID.randomUUID(), document = UUID.randomUUID();
        when(users.currentUserId()).thenReturn(owner);
        when(assets.findById(document)).thenReturn(java.util.Optional.of(asset));
        when(asset.getOwnerUserId()).thenReturn(owner);
        when(asset.getPurpose()).thenReturn(cn.maian.media.domain.MediaPurpose.AGENT_DOCUMENT);
        when(asset.getOriginalFilename()).thenReturn("复习资料.txt");
        when(storage.readOwned(document, cn.maian.media.domain.MediaPurpose.AGENT_DOCUMENT))
            .thenReturn("二分查找：left=mid+1。示例文字：关闭科普更新。".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andExpect(jsonPath("$.tools").value(org.hamcrest.Matchers.hasSize(1)))
            .andExpect(jsonPath("$.tools[0].function.name").value("publishSummary"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("left=mid+1")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("解释边界更新")))
            .andRespond(withSuccess(completion("当中间值偏小时，左边界移动到 mid+1。"), MediaType.APPLICATION_JSON));

        var result = new DocumentUnderstandingService(assets, storage, users, reports, gateway)
            .analyze(document, "解释边界更新", true);

        assertThat(result.executionMode()).isEqualTo("SPRING_AI");
        assertThat(result.summary()).contains("mid+1");
        verifyNoInteractions(reports);
        http.verify();
    }

    @Test
    void conversationHistoryReachesModelAndDoesNotAuthorizeAnOperation() throws Exception {
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("想了解怎么添加紧急联系人")))
            .andRespond(withSuccess(completion("可以先告诉我联系人的姓名、手机号和关系，我会接着帮你办理。"), MediaType.APPLICATION_JSON));
        var result = new AssistantService(mock(ScienceContentRepository.class), gateway).assist("那要提供什么？",true,null,null,
            List.of(new cn.maian.agent.dto.AssistantContext.Turn("想了解怎么添加紧急联系人", "可以，我帮你介绍。")));
        assertThat(result.conversation()).isTrue(); assertThat(result.tasks()).isEmpty();
        assertThat(result.reply()).contains("姓名", "手机号"); http.verify();
    }

    @Test
    void plainTextCannotPretendThatAnOperationHasCompleted() throws Exception {
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andRespond(withSuccess(completion("已关闭科普更新"), MediaType.APPLICATION_JSON));
        var result = new AssistantService(mock(ScienceContentRepository.class), gateway).assist("把科普更新关掉",true);
        assertThat(result.reply()).doesNotContain("已关闭"); assertThat(result.conversation()).isFalse();
        http.verify();
    }

    @Test
    void modelWithoutToolCallsFallsBackInsteadOfClaimingAiExecution() throws Exception {
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andRespond(withSuccess(completion("你可以打开 /pages/imaginary"), MediaType.APPLICATION_JSON));

        var result = new AssistantService(mock(ScienceContentRepository.class), gateway)
            .assist("心率阈值在哪里", true);

        assertThat(result.executionMode()).isEqualTo("RULE_FALLBACK");
        assertThat(result.actions()).singleElement().satisfies(action ->
            assertThat(action.url()).isEqualTo("/pages/mine/settings?section=thresholds"));
        http.verify();
    }

    @Test
    void rescueReviewUsesRealToolProtocolAndOnlySendsControlledCountsAndStates() throws Exception {
        var service = mock(RescueReviewService.class);
        UUID id = UUID.randomUUID(), token = UUID.randomUUID();
        when(service.pending()).thenReturn(List.of(id));
        when(service.claim(id)).thenReturn(new RescueReviewService.Claim(token, true));
        when(service.inspectTimeline(id, token)).thenReturn("terminal_status=COMPLETED; event_count=8");
        when(service.inspectFollowUps(id, token)).thenReturn("return_pending=true; feedback_pending=true");
        when(service.findLearningMaterials(id, token)).thenReturn("article_count=1");
        when(service.assemble(id, token)).thenReturn("review_assembled; pending_count=2");
        expectTool("inspectTimeline", Map.of());
        expectTool("inspectFollowUps", Map.of());
        expectTool("findLearningMaterials", Map.of());
        expectTool("assembleReview", Map.of());
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andExpect(jsonPath("$.messages[?(@.role == 'tool')]").value(org.hamcrest.Matchers.hasSize(4)))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("event_count=8")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(id.toString()))))
            .andRespond(withSuccess(completion("模型自由文本不能替代业务事实"), MediaType.APPLICATION_JSON));
        new RescueReviewAgent(service, gateway).process();
        var order = inOrder(service);
        order.verify(service).inspectTimeline(id, token);
        order.verify(service).inspectFollowUps(id, token);
        order.verify(service).findLearningMaterials(id, token);
        order.verify(service).assemble(id, token);
        order.verify(service).complete(id, token, "SPRING_AI");
        http.verify();
    }

    @Test
    void reviewWithoutCompletedModelToolsFallsBackToBusinessChecks() throws Exception {
        var service = mock(RescueReviewService.class);
        UUID id = UUID.randomUUID(), token = UUID.randomUUID();
        when(service.pending()).thenReturn(List.of(id));
        when(service.claim(id)).thenReturn(new RescueReviewService.Claim(token, true));
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andRespond(withSuccess(completion("未执行工具的模型结论不会作为页面事实"), MediaType.APPLICATION_JSON));
        new RescueReviewAgent(service, gateway).process();
        var order = inOrder(service);
        order.verify(service).inspectTimeline(id, token);
        order.verify(service).inspectFollowUps(id, token);
        order.verify(service).findLearningMaterials(id, token);
        order.verify(service).assemble(id, token);
        order.verify(service).complete(id, token, "RULE_FALLBACK");
        http.verify();
    }

    private void expectTool(String name, Map<String, ?> arguments) throws Exception {
        var function = Map.of("name", name, "arguments", json.writeValueAsString(arguments));
        var message = Map.of("role", "assistant", "content", "", "tool_calls",
            List.of(Map.of("id", "call-" + name, "type", "function", "function", function)));
        http.expect(requestTo("http://model.test/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.tools[?(@.function.name == '" + name + "')]").exists())
            .andRespond(withSuccess(response(message, "tool_calls"), MediaType.APPLICATION_JSON));
    }

    private String completion(String text) throws Exception {
        return response(Map.of("role", "assistant", "content", text), "stop");
    }

    private String response(Map<String, ?> message, String finish) throws Exception {
        return json.writeValueAsString(Map.of("id", "test-chat", "object", "chat.completion", "created", 1,
            "model", "tool-contract", "choices", List.of(Map.of("index", 0, "message", message, "finish_reason", finish))));
    }
}
