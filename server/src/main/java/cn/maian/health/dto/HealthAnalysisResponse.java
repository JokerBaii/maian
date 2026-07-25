package cn.maian.health.dto;

import cn.maian.health.domain.HealthRiskLevel;

import java.util.List;

public record HealthAnalysisResponse(
    HealthRiskLevel riskLevel,
    String summary,
    List<String> abnormalItems,
    List<String> recommendations,
    String disclaimer,
    String analysisSource
) {
    public static final String MEDICAL_DISCLAIMER =
        "本结果仅用于健康信息整理，不能替代医生诊断；如有胸痛、呼吸困难、意识异常等急症表现，请立即呼叫急救。";

    public HealthAnalysisResponse {
        abnormalItems = abnormalItems == null ? List.of() : List.copyOf(abnormalItems);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
