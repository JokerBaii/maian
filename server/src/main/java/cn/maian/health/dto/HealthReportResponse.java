package cn.maian.health.dto;

import cn.maian.health.domain.HealthReport;
import cn.maian.health.domain.HealthRiskLevel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HealthReportResponse(
    UUID id,
    LocalDate checkupDate,
    String hospital,
    UUID sourceMediaId,
    HealthRiskLevel riskLevel,
    String summary,
    List<HealthIndicator> indicators,
    List<String> recommendations,
    String disclaimer,
    String analysisSource,
    Instant createdAt
) {
    public static HealthReportResponse from(HealthReport report) {
        return new HealthReportResponse(
            report.getId(),
            report.getCheckupDate(),
            report.getHospital(),
            report.getSourceMediaId(),
            report.getRiskLevel(),
            report.getSummary(),
            report.getIndicators().stream().map(indicator -> indicator.toDto()).toList(),
            List.copyOf(report.getRecommendations()),
            report.getDisclaimer(),
            report.getAnalysisSource(),
            report.getCreatedAt()
        );
    }
}
