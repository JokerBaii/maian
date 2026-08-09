package cn.maian.health.dto;

import java.time.LocalDate;
import java.util.List;


public record ReportInterpretation(
    String hospital,
    LocalDate checkupDate,
    List<HealthIndicator> indicators
) {
}
