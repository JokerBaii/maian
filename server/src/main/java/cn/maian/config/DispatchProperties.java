package cn.maian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "app.dispatch")
public record DispatchProperties(
    @DecimalMin("0.1") double searchRadiusKm,
    @Min(1) @Max(250) int candidateLimitPerType,
    @Min(1) long mobileLocationMaxAgeSeconds,
    @DecimalMin("0.1") double mobileSpeedKmh,
    @DecimalMin("0.1") double runnerSpeedKmh,
    @DecimalMin("1.0") double routeDistanceFactor,
    @DecimalMin("0.1") double volunteerOfferRadiusKm,
    @Min(15) long volunteerPresenceMaxAgeSeconds,
    @Min(0) int mobileDispatchOverheadSeconds,
    @Min(0) int fixedPickupOverheadSeconds
) {
}
