package cn.maian.media.dto;

import java.time.Instant;

public record MediaDownloadResponse(String url, Instant expiresAt) {
}
