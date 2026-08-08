package cn.maian.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.abuse")
public record AbuseProperties(
    @Min(1) int windowSeconds,
    @Min(1) int rescueUserLimit,
    @Min(1) int rescueIpLimit,
    @Min(1) int mediaUserLimit,
    @Min(1) int mobileHeartbeatUserLimit
) {
}
