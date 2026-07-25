package cn.maian.health.dto;

import jakarta.validation.constraints.NotBlank;

public record HealthIndicator(
    @NotBlank(message = "指标名称不能为空")
    String name,
    @NotBlank(message = "指标值不能为空")
    String value,
    String unit,
    String referenceRange,
    boolean abnormal
) {
}
