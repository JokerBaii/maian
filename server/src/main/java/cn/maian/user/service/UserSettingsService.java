package cn.maian.user.service;

import cn.maian.user.domain.UserSettings;
import cn.maian.user.dto.UpdateUserSettingsRequest;
import cn.maian.user.dto.UserSettingsResponse;
import cn.maian.user.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;

    public UserSettingsService(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    @Transactional
    public UserSettingsResponse current() {
        return UserSettingsResponse.from(findOrCreate());
    }

    @Transactional
    public UserSettingsResponse update(UpdateUserSettingsRequest request) {
        if (request.minHeartRate() >= request.maxHeartRate()) {
            throw new IllegalArgumentException("最低心率必须小于最高心率");
        }
        var settings = findOrCreate();
        settings.update(
            request.rescuePush(),
            request.healthAlert(),
            request.scienceUpdate(),
            request.locationShare(),
            request.healthDataShare(),
            request.maxHeartRate(),
            request.minHeartRate()
        );
        return UserSettingsResponse.from(settings);
    }

    @Transactional
    public UserSettings findOrCreate() {
        return userSettingsRepository.findById(UserProfileService.CURRENT_USER_ID)
            .orElseGet(() -> userSettingsRepository.save(
                UserSettings.defaults(UserProfileService.CURRENT_USER_ID)
            ));
    }
}
