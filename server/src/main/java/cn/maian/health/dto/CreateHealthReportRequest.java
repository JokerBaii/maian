package cn.maian.health.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateHealthReportRequest(
    @NotNull @PastOrPresent LocalDate checkupDate,
    @NotBlank @Size(max = 120) String hospital,
    UUID sourceMediaId,
    @NotEmpty @Size(max = 50) List<@Valid HealthIndicator> indicators
) {
}
