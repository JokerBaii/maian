package cn.maian.media.service;

import cn.maian.config.AuthProperties;
import cn.maian.config.MediaProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class MediaTokenService {

    private final byte[] secret;
    private final MediaProperties properties;
    private final Clock clock;

    public MediaTokenService(AuthProperties authProperties, MediaProperties properties, Clock clock) {
        this.secret = authProperties.secret().getBytes(StandardCharsets.UTF_8);
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken issue(UUID mediaId) {
        Instant expiresAt = clock.instant().plusSeconds(properties.downloadTokenMinutes() * 60);
        String payload = mediaId + "." + expiresAt.getEpochSecond();
        String token = encode(payload.getBytes(StandardCharsets.UTF_8)) + "." + encode(sign(payload));
        return new IssuedToken(token, expiresAt);
    }

    public void validate(UUID mediaId, String token) {
        try {
            String[] parts = token.split("\\.", 2);
            if (parts.length != 2) {
                throw invalid();
            }
            String payload = new String(decode(parts[0]), StandardCharsets.UTF_8);
            byte[] providedSignature = decode(parts[1]);
            if (!MessageDigest.isEqual(providedSignature, sign(payload))) {
                throw invalid();
            }
            int separator = payload.lastIndexOf('.');
            UUID tokenMediaId = UUID.fromString(payload.substring(0, separator));
            long expiresAt = Long.parseLong(payload.substring(separator + 1));
            if (!mediaId.equals(tokenMediaId) || clock.instant().getEpochSecond() > expiresAt) {
                throw invalid();
            }
        } catch (RuntimeException exception) {
            if (exception instanceof IllegalArgumentException
                && "媒体访问链接无效或已过期".equals(exception.getMessage())) {
                throw exception;
            }
            throw invalid();
        }
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("无法签发媒体访问令牌", exception);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private IllegalArgumentException invalid() {
        return new IllegalArgumentException("媒体访问链接无效或已过期");
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
