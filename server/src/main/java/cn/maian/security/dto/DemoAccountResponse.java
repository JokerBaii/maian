package cn.maian.security.dto;

import cn.maian.user.domain.UserProfile;

import java.util.UUID;

public record DemoAccountResponse(
    UUID id,
    String nickname,
    String role,
    String avatarUrl
) {
    public static DemoAccountResponse from(UserProfile profile) {
        return new DemoAccountResponse(
            profile.getId(),
            profile.getNickname(),
            profile.getRole(),
            profile.getAvatarUrl()
        );
    }
}
