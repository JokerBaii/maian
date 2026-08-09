package cn.maian.health.dto;

import java.time.LocalDate;
import java.util.List;


public record ReportRecognitionResponse(
    String notice,
    String hospital,
    LocalDate checkupDate,
    List<HealthIndicator> indicators,
    List<String> rawLines
) {
    public static final String REVIEW_NOTICE = "已根据检查单自动整理报告内容。";
}
