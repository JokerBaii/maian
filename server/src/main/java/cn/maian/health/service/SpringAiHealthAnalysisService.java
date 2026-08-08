package cn.maian.health.service;

import cn.maian.health.dto.HealthAnalysisRequest;
import cn.maian.health.dto.HealthAnalysisResponse;
import cn.maian.health.dto.HealthIndicator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import cn.maian.user.service.UserSettingsService;

import java.util.stream.Collectors;

@Service
@Primary
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class SpringAiHealthAnalysisService implements HealthAnalysisService {

    private final ChatClient chatClient;
    private final Resource promptTemplate;
    private final RuleBasedHealthAnalysisService fallback;
    private final UserSettingsService userSettingsService;

    public SpringAiHealthAnalysisService(
        ChatClient.Builder chatClientBuilder,
        @Value("classpath:prompts/analyze-health-report.st") Resource promptTemplate,
        RuleBasedHealthAnalysisService fallback,
        UserSettingsService userSettingsService
    ) {
        this.chatClient = chatClientBuilder.build();
        this.promptTemplate = promptTemplate;
        this.fallback = fallback;
        this.userSettingsService = userSettingsService;
    }

    @Override
    public HealthAnalysisResponse analyze(HealthAnalysisRequest request) {
        if (!userSettingsService.findOrCreate().isHealthDataShare()) {
            return fallback.analyze(request);
        }
        HealthAnalysisResponse result;
        try {
            result = chatClient.prompt()
                .user(user -> user
                    .text(promptTemplate)
                    .param("patientSummary", request.patientSummary())
                    .param("indicators", formatIndicators(request)))
                .call()
                .entity(HealthAnalysisResponse.class);
        } catch (Exception exception) {
            // AI 不可用（余额不足、网络中断、超时）时自动降级到规则分析，
            // 保证体检报告保存不失败。规则结果 analysisSource 为 RULE_BASED。
            return fallback.analyze(request);
        }

        if (result == null) {
            return fallback.analyze(request);
        }

        return new HealthAnalysisResponse(
            result.riskLevel(),
            result.summary(),
            result.abnormalItems(),
            result.recommendations(),
            HealthAnalysisResponse.MEDICAL_DISCLAIMER,
            "SPRING_AI"
        );
    }

    private String formatIndicators(HealthAnalysisRequest request) {
        return request.indicators().stream()
            .map(this::formatIndicator)
            .collect(Collectors.joining("\n"));
    }

    private String formatIndicator(HealthIndicator indicator) {
        return "- %s: %s %s；参考范围：%s；是否标记异常：%s".formatted(
            indicator.name(),
            indicator.value(),
            valueOrEmpty(indicator.unit()),
            valueOrEmpty(indicator.referenceRange()),
            indicator.abnormal() ? "是" : "否"
        );
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
