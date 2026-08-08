package cn.maian.media.dto;

import java.time.Instant;
import java.util.UUID;

public record MediaUploadResponse(
    UUID mediaId,
    String contentType,
    long sizeBytes,
    String downloadUrl,
    Instant downloadUrlExpiresAt
) {
}
