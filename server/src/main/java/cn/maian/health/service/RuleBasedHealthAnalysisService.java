package cn.maian.health.service;

import cn.maian.health.domain.HealthRiskLevel;
import cn.maian.health.dto.HealthAnalysisRequest;
import cn.maian.health.dto.HealthAnalysisResponse;
import cn.maian.health.dto.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(
    name = "app.ai.enabled",
    havingValue = "false",
    matchIfMissing = true
)
public class RuleBasedHealthAnalysisService implements HealthAnalysisService {

    @Override
    public HealthAnalysisResponse analyze(HealthAnalysisRequest request) {
        List<String> abnormalItems = request.indicators().stream()
            .filter(HealthIndicator::abnormal)
            .map(this::describe)
            .toList();

        HealthRiskLevel riskLevel = switch (abnormalItems.size()) {
            case 0 -> HealthRiskLevel.LOW;
            case 1, 2 -> HealthRiskLevel.MEDIUM;
            default -> HealthRiskLevel.HIGH;
        };

        String summary = abnormalItems.isEmpty()
            ? "本次提交的指标均未标记为异常，请继续保持规律监测。"
            : "发现 " + abnormalItems.size() + " 项被标记为异常，建议结合症状和既往病史进一步评估。";

        List<String> recommendations = abnormalItems.isEmpty()
            ? List.of("保持规律作息和适量运动", "按既定周期复查健康指标")
            : List.of("记录异常指标出现的时间和伴随症状", "携带完整报告咨询医生", "症状明显加重时及时就医");

        return new HealthAnalysisResponse(
            riskLevel,
            summary,
            abnormalItems,
            recommendations,
            HealthAnalysisResponse.MEDICAL_DISCLAIMER,
            "RULE_BASED"
        );
    }

    private String describe(HealthIndicator indicator) {
        String unit = indicator.unit() == null ? "" : " " + indicator.unit();
        String range = indicator.referenceRange() == null
            ? ""
            : "（参考范围：" + indicator.referenceRange() + "）";
        return indicator.name() + "：" + indicator.value() + unit + range;
    }
}
