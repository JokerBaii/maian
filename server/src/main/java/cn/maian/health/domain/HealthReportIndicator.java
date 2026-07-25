package cn.maian.health.domain;

import cn.maian.health.dto.HealthIndicator;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class HealthReportIndicator {

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "value_text", length = 100, nullable = false)
    private String value;

    @Column(length = 50)
    private String unit;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Column(nullable = false)
    private boolean abnormal;

    protected HealthReportIndicator() {
    }

    public static HealthReportIndicator from(HealthIndicator indicator) {
        var result = new HealthReportIndicator();
        result.name = indicator.name().trim();
        result.value = indicator.value().trim();
        result.unit = normalize(indicator.unit());
        result.referenceRange = normalize(indicator.referenceRange());
        result.abnormal = indicator.abnormal();
        return result;
    }

    public HealthIndicator toDto() {
        return new HealthIndicator(name, value, unit, referenceRange, abnormal);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
