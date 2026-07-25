package cn.maian.health.domain;

import cn.maian.health.dto.HealthAnalysisResponse;
import cn.maian.health.dto.HealthIndicator;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "health_reports")
public class HealthReport {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDate checkupDate;

    @Column(length = 120, nullable = false)
    private String hospital;

    @Column(length = 500)
    private String sourceImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private HealthRiskLevel riskLevel;

    @Column(length = 1000, nullable = false)
    private String summary;

    @Column(length = 1000, nullable = false)
    private String disclaimer;

    @Column(length = 30, nullable = false)
    private String analysisSource;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "health_report_indicators",
        joinColumns = @JoinColumn(name = "report_id")
    )
    @OrderColumn(name = "position")
    private List<HealthReportIndicator> indicators = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "health_report_recommendations",
        joinColumns = @JoinColumn(name = "report_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "recommendation", length = 500, nullable = false)
    private List<String> recommendations = new ArrayList<>();

    protected HealthReport() {
    }

    public static HealthReport create(
        UUID userId,
        LocalDate checkupDate,
        String hospital,
        String sourceImageUrl,
        List<HealthIndicator> indicators,
        HealthAnalysisResponse analysis
    ) {
        var report = new HealthReport();
        report.id = UUID.randomUUID();
        report.userId = userId;
        report.checkupDate = checkupDate;
        report.hospital = hospital.trim();
        report.sourceImageUrl = normalize(sourceImageUrl);
        report.riskLevel = analysis.riskLevel();
        report.summary = analysis.summary();
        report.disclaimer = analysis.disclaimer();
        report.analysisSource = analysis.analysisSource();
        report.createdAt = Instant.now();
        report.indicators = indicators.stream().map(HealthReportIndicator::from).toList();
        report.recommendations = List.copyOf(analysis.recommendations());
        return report;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getCheckupDate() {
        return checkupDate;
    }

    public String getHospital() {
        return hospital;
    }

    public String getSourceImageUrl() {
        return sourceImageUrl;
    }

    public HealthRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getSummary() {
        return summary;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public String getAnalysisSource() {
        return analysisSource;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<HealthReportIndicator> getIndicators() {
        return indicators;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }
}
