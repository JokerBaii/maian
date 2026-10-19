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

    private void expectTool(String name, Map<String, String> arguments) throws Exception {
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
