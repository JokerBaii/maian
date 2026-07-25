package cn.maian.health.service;

import cn.maian.common.exception.HealthAnalysisUnavailableException;
import cn.maian.health.dto.HealthAnalysisRequest;
import cn.maian.health.dto.HealthAnalysisResponse;
import cn.maian.health.dto.HealthIndicator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class SpringAiHealthAnalysisService implements HealthAnalysisService {

    private final ChatClient chatClient;
    private final Resource promptTemplate;

    public SpringAiHealthAnalysisService(
        ChatClient.Builder chatClientBuilder,
        @Value("classpath:prompts/analyze-health-report.st") Resource promptTemplate
    ) {
        this.chatClient = chatClientBuilder.build();
        this.promptTemplate = promptTemplate;
    }

    @Override
    public HealthAnalysisResponse analyze(HealthAnalysisRequest request) {
        HealthAnalysisResponse result;
        try {
            result = chatClient.prompt()
                .user(user -> user
                    .text(promptTemplate)
                    .param("patientSummary", request.patientSummary())
                    .param("indicators", formatIndicators(request)))
                .call()
                .entity(HealthAnalysisResponse.class);
        } catch (TransientAiException exception) {
            throw new HealthAnalysisUnavailableException(
                "AI 健康分析暂时繁忙，请稍后重试",
                exception
            );
        } catch (NonTransientAiException exception) {
            throw new HealthAnalysisUnavailableException(
                "AI 健康分析配置不可用，请联系管理员",
                exception
            );
        }

        if (result == null) {
            throw new HealthAnalysisUnavailableException(
                "AI 未返回可解析的健康分析结果",
                null
            );
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
