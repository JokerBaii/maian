package cn.maian.health.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 体检报告识别结果。
 *
 * 识别到的指标一律需要人工核对后才能保存，notice 承载该提示。
 */
public record ReportRecognitionResponse(
    String notice,
    String hospital,
    LocalDate checkupDate,
    List<HealthIndicator> indicators,
    List<String> rawLines
) {
    public static final String REVIEW_NOTICE = "已识别以下指标，请逐项核对后保存。";
}
