package cn.maian.health.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReportRecognitionRequest(@NotNull UUID mediaId) {
}
