package cn.maian.security.dto;

import java.time.Instant;

public record AuthTokenResponse(String accessToken, Instant expiresAt) {
}
