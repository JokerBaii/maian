package cn.maian.user.dto;

import cn.maian.user.domain.UserProfile;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
    UUID id,
    String nickname,
    String phone,
    String role,
    boolean verified,
    Instant createdAt
) {
    public static AdminUserResponse from(UserProfile profile) {
        return new AdminUserResponse(
            profile.getId(),
            profile.getNickname(),
            profile.getPhone(),
            profile.getRole(),
            profile.isVerified(),
            profile.getCreatedAt()
        );
    }
}
