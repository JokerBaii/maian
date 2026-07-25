package cn.maian.health.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HealthAnalysisRequest(
    @NotBlank(message = "健康概况不能为空")
    @Size(max = 2000, message = "健康概况不能超过 2000 字")
    String patientSummary,
    @NotEmpty(message = "至少需要一个健康指标")
    @Size(max = 50, message = "单次最多分析 50 个指标")
    List<@Valid HealthIndicator> indicators
) {
}
