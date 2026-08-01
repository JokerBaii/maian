package cn.maian.health.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public record CreateHeartRateReadingRequest(
    @Min(25) @Max(250) int bpm,
    @Pattern(regexp = "resting|exercise|sleeping") String scene,
    Instant recordedAt
) {
}
