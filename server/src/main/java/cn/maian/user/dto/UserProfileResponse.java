package cn.maian.user.dto;

import cn.maian.user.domain.UserProfile;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String nickname,
    String phone,
    String role,
    String avatarUrl,
    String realName,
    String idCard,
    boolean verified,
    Instant createdAt
) {
    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
            profile.getId(),
            profile.getNickname(),
            profile.getPhone(),
            profile.getRole(),
            profile.getAvatarUrl(),
            profile.getRealName(),
            profile.getIdCardMasked(),
            profile.isVerified(),
            profile.getCreatedAt()
        );
    }
}
