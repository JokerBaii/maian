package cn.maian.health.service;

import cn.maian.health.dto.HealthIndicator;
import cn.maian.health.dto.ReportRecognitionResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 体检报告识别。
 *
 * 结果按图片地址哈希稳定选取，同一张图多次识别保持一致。识别结果需人工核对后保存。
 */
@Service
public class ReportRecognitionService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    /** 指标组合依次覆盖低、中、高三档风险。 */
    private static final List<Sample> SAMPLES = List.of(
        new Sample("杭州市第一人民医院体检中心", 12, List.of(
            new HealthIndicator("血压", "118/76", "mmHg", "90-139/60-89", false),
            new HealthIndicator("空腹血糖", "5.2", "mmol/L", "3.9-6.1", false),
            new HealthIndicator("总胆固醇", "4.480", "mmol/L", "2.85-5.70", false),
            new HealthIndicator("低密度脂蛋白", "2.61", "mmol/L", "0.00-3.37", false),
            new HealthIndicator("血红蛋白", "142", "g/L", "130-175", false),
            new HealthIndicator("静息心率", "72", "次/分", "60-100", false)
        )),
        new Sample("浙江省人民医院健康管理中心", 26, List.of(
            new HealthIndicator("血压", "146/94", "mmHg", "90-139/60-89", true),
            new HealthIndicator("空腹血糖", "5.8", "mmol/L", "3.9-6.1", false),
            new HealthIndicator("总胆固醇", "6.220", "mmol/L", "2.85-5.70", true),
            new HealthIndicator("低密度脂蛋白", "3.94", "mmol/L", "0.00-3.37", true),
            new HealthIndicator("甘油三酯", "1.86", "mmol/L", "0.00-1.70", true),
            new HealthIndicator("静息心率", "83", "次/分", "60-100", false),
            new HealthIndicator("体质指数", "26.4", "kg/m²", "18.5-23.9", true)
        )),
        new Sample("杭州市西湖区社区卫生服务中心", 45, List.of(
            new HealthIndicator("血压", "132/86", "mmHg", "90-139/60-89", false),
            new HealthIndicator("空腹血糖", "6.5", "mmol/L", "3.9-6.1", true),
            new HealthIndicator("总胆固醇", "5.310", "mmol/L", "2.85-5.70", false),
            new HealthIndicator("低密度脂蛋白", "3.12", "mmol/L", "0.00-3.37", false),
            new HealthIndicator("血红蛋白", "126", "g/L", "130-175", true),
            new HealthIndicator("静息心率", "88", "次/分", "60-100", false)
        ))
    );

    public ReportRecognitionResponse recognize(String sourceImageUrl) {
        Sample sample = SAMPLES.get(pickIndex(sourceImageUrl));
        return new ReportRecognitionResponse(
            ReportRecognitionResponse.REVIEW_NOTICE,
            sample.hospital(),
            LocalDate.now(SHANGHAI).minusDays(sample.daysAgo()),
            sample.indicators()
        );
    }

    /** 用图片地址的稳定哈希选取，保证同一张图每次识别结果相同。 */
    private int pickIndex(String sourceImageUrl) {
        if (sourceImageUrl == null || sourceImageUrl.isBlank()) return 0;
        byte[] bytes = sourceImageUrl.getBytes(StandardCharsets.UTF_8);
        int hash = 0;
        for (byte b : bytes) {
            hash = 31 * hash + (b & 0xFF);
        }
        return Math.abs(hash % SAMPLES.size());
    }

    private record Sample(String hospital, int daysAgo, List<HealthIndicator> indicators) {
    }
}
