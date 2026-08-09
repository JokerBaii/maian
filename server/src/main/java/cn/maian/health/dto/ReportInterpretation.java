package cn.maian.health.dto;

import java.time.LocalDate;
import java.util.List;

/** DeepSeek 对 OCR 原文的结构化结果；服务层仍会校验并限制数量。 */
public record ReportInterpretation(
    String hospital,
    LocalDate checkupDate,
    List<HealthIndicator> indicators
) {
}
