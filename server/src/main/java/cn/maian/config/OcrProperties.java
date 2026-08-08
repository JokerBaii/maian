package cn.maian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ocr")
public record OcrProperties(boolean enabled, String apiKey, String secretKey) {
}
