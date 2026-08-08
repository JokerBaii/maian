package cn.maian.rescue.dto;

import cn.maian.user.domain.UserProfile;

import java.util.UUID;

public record RescueParticipantResponse(
    UUID userId,
    String displayName,
    String phone,
    boolean verified
) {
    public static RescueParticipantResponse from(UserProfile profile) {
        String displayName = profile.getRealName() == null || profile.getRealName().isBlank()
            ? profile.getNickname()
            : profile.getRealName();
        return new RescueParticipantResponse(
            profile.getId(), displayName, profile.getPhone(), profile.isVerified()
        );
    }
}
