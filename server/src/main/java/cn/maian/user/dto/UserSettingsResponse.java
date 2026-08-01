package cn.maian.user.dto;

import cn.maian.user.domain.UserSettings;

import java.time.Instant;

public record UserSettingsResponse(
    boolean rescuePush,
    boolean healthAlert,
    boolean scienceUpdate,
    boolean locationShare,
    boolean healthDataShare,
    int maxHeartRate,
    int minHeartRate,
    Instant updatedAt
) {
    public static UserSettingsResponse from(UserSettings settings) {
        return new UserSettingsResponse(
            settings.isRescuePush(),
            settings.isHealthAlert(),
            settings.isScienceUpdate(),
            settings.isLocationShare(),
            settings.isHealthDataShare(),
            settings.getMaxHeartRate(),
            settings.getMinHeartRate(),
            settings.getUpdatedAt()
        );
    }
}
