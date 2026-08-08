package cn.maian.health.service;

import cn.maian.common.exception.ExternalRecognitionUnavailableException;
import cn.maian.common.exception.ForbiddenOperationException;
import cn.maian.config.OcrProperties;
import cn.maian.health.dto.HealthIndicator;
import cn.maian.health.dto.ReportRecognitionResponse;
import cn.maian.media.domain.MediaPurpose;
import cn.maian.media.service.MediaStorageService;
import cn.maian.user.service.UserSettingsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ReportRecognitionService {

    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(20\\d{2})[\\-/.\u5e74](\\d{1,2})[\\-/.\u6708](\\d{1,2})(?:\u65e5)?"
    );
    private static final Pattern RANGE_PATTERN = Pattern.compile(
        "([0-9]+(?:\\.[0-9]+)?\\s*[-~—至]\\s*[0-9]+(?:\\.[0-9]+)?)"
    );
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+(?:\\.[0-9]+)?");
    private static final Pattern PRESSURE_PATTERN = Pattern.compile("(\\d{2,3})\\s*/\\s*(\\d{2,3})");
    private static final List<IndicatorSpec> INDICATORS = List.of(
        new IndicatorSpec("血压", List.of("血压"), "mmHg"),
        new IndicatorSpec("空腹血糖", List.of("空腹血糖", "GLU"), "mmol/L"),
        new IndicatorSpec("总胆固醇", List.of("总胆固醇", "TC"), "mmol/L"),
        new IndicatorSpec("低密度脂蛋白", List.of("低密度脂蛋白", "LDL-C", "LDL"), "mmol/L"),
        new IndicatorSpec("高密度脂蛋白", List.of("高密度脂蛋白", "HDL-C", "HDL"), "mmol/L"),
        new IndicatorSpec("甘油三酯", List.of("甘油三酯", "TG"), "mmol/L"),
        new IndicatorSpec("血红蛋白", List.of("血红蛋白", "HGB"), "g/L"),
        new IndicatorSpec("静息心率", List.of("静息心率", "心率"), "次/分"),
        new IndicatorSpec("体质指数", List.of("体质指数", "BMI"), "kg/m²")
    );

    private final OcrProperties properties;
    private final MediaStorageService mediaStorageService;
    private final UserSettingsService userSettingsService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private volatile AccessToken cachedToken;

    public ReportRecognitionService(
        OcrProperties properties,
        MediaStorageService mediaStorageService,
        UserSettingsService userSettingsService,
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.properties = properties;
        this.mediaStorageService = mediaStorageService;
        this.userSettingsService = userSettingsService;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public ReportRecognitionResponse recognize(UUID mediaId) {
        requireConsentAndConfiguration();
        byte[] image = mediaStorageService.readOwned(mediaId, MediaPurpose.HEALTH_REPORT);
        List<String> lines = callBaidu(image);
        List<HealthIndicator> indicators = parseIndicators(lines);
        if (indicators.isEmpty()) {
            throw new ExternalRecognitionUnavailableException(
                "OCR 已返回文字，但未识别出可结构化的健康指标，请手动核对录入"
            );
        }
        return new ReportRecognitionResponse(
            ReportRecognitionResponse.REVIEW_NOTICE,
            findHospital(lines),
            findDate(lines),
            indicators,
            lines
        );
    }

    private void requireConsentAndConfiguration() {
        if (!userSettingsService.findOrCreate().isHealthDataShare()) {
            throw new ForbiddenOperationException("未授权将健康报告发送至百度智能云 OCR");
        }
        if (!properties.enabled() || blank(properties.apiKey()) || blank(properties.secretKey())) {
            throw new ExternalRecognitionUnavailableException(
                "百度智能云 OCR 未配置，请手动录入体检指标"
            );
        }
    }

    private List<String> callBaidu(byte[] image) {
        try {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("image", Base64.getEncoder().encodeToString(image));
            form.add("detect_direction", "true");
            String body = restClient.post()
                .uri("https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic?access_token={token}", accessToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            if (root.has("error_code")) {
                throw new ExternalRecognitionUnavailableException(
                    "百度智能云 OCR 返回错误：" + root.path("error_msg").asText("未知错误")
                );
            }
            List<String> lines = new ArrayList<>();
            root.path("words_result").forEach(item -> {
                String words = item.path("words").asText().trim();
                if (!words.isBlank()) {
                    lines.add(words);
                }
            });
            return List.copyOf(lines);
        } catch (ExternalRecognitionUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ExternalRecognitionUnavailableException(
                "百度智能云 OCR 暂时不可用，请手动录入或稍后重试", exception
            );
        }
    }

    private synchronized String accessToken() throws Exception {
        Instant now = clock.instant();
        if (cachedToken != null && cachedToken.expiresAt().isAfter(now.plusSeconds(60))) {
            return cachedToken.value();
        }
        String body = restClient.post()
            .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("aip.baidubce.com")
                .path("/oauth/2.0/token")
                .queryParam("grant_type", "client_credentials")
                .queryParam("client_id", properties.apiKey())
                .queryParam("client_secret", properties.secretKey())
                .build())
            .retrieve()
            .body(String.class);
        JsonNode root = objectMapper.readTree(body);
        String value = root.path("access_token").asText();
        long expiresIn = root.path("expires_in").asLong(0);
        if (value.isBlank() || expiresIn <= 0) {
            throw new ExternalRecognitionUnavailableException("百度智能云 OCR 鉴权失败");
        }
        cachedToken = new AccessToken(value, now.plusSeconds(expiresIn));
        return value;
    }

    private List<HealthIndicator> parseIndicators(List<String> lines) {
        List<HealthIndicator> result = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();
        for (IndicatorSpec spec : INDICATORS) {
            for (String line : lines) {
                String alias = spec.aliases().stream().filter(line::contains).findFirst().orElse(null);
                if (alias == null || !added.add(spec.name())) {
                    continue;
                }
                String value = extractValue(spec.name(), alias, line);
                if (value == null) {
                    added.remove(spec.name());
                    continue;
                }
                String tail = line.substring(Math.min(line.length(), line.indexOf(alias) + alias.length()));
                var rangeMatcher = RANGE_PATTERN.matcher(tail);
                String range = rangeMatcher.find() ? rangeMatcher.group(1).replaceAll("\\s+", "") : null;
                boolean abnormal = line.contains("↑") || line.contains("↓") || line.contains("异常")
                    || line.matches(".*(?:\\sH|\\sL|\\*).*?");
                result.add(new HealthIndicator(spec.name(), value, spec.unit(), range, abnormal));
                break;
            }
        }
        return List.copyOf(result);
    }

    private String extractValue(String name, String alias, String line) {
        String tail = line.substring(Math.min(line.length(), line.indexOf(alias) + alias.length()));
        if ("血压".equals(name)) {
            var pressure = PRESSURE_PATTERN.matcher(tail);
            return pressure.find() ? pressure.group(1) + "/" + pressure.group(2) : null;
        }
        var number = NUMBER_PATTERN.matcher(tail);
        return number.find() ? number.group() : null;
    }

    private String findHospital(List<String> lines) {
        return lines.stream()
            .filter(line -> line.contains("医院") || line.contains("体检中心") || line.contains("健康管理中心"))
            .findFirst()
            .orElse("待手动核对");
    }

    private LocalDate findDate(List<String> lines) {
        for (String line : lines) {
            var matcher = DATE_PATTERN.matcher(line);
            if (matcher.find()) {
                try {
                    return LocalDate.of(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3))
                    );
                } catch (RuntimeException ignored) {
                    // Keep scanning OCR lines for another valid date.
                }
            }
        }
        return LocalDate.now(clock);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record IndicatorSpec(String name, List<String> aliases, String unit) {
    }

    private record AccessToken(String value, Instant expiresAt) {
    }
}
