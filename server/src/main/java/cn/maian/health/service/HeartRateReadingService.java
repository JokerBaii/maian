package cn.maian.health.service;

import cn.maian.health.domain.HeartRateReading;
import cn.maian.health.dto.CreateHeartRateReadingRequest;
import cn.maian.health.dto.HeartRateReadingResponse;
import cn.maian.health.repository.HeartRateReadingRepository;
import cn.maian.user.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class HeartRateReadingService {

    private final HeartRateReadingRepository heartRateReadingRepository;
    private final WearableDeviceService wearableDeviceService;

    public HeartRateReadingService(
        HeartRateReadingRepository heartRateReadingRepository,
        WearableDeviceService wearableDeviceService
    ) {
        this.heartRateReadingRepository = heartRateReadingRepository;
        this.wearableDeviceService = wearableDeviceService;
    }

    @Transactional
    public HeartRateReadingResponse create(CreateHeartRateReadingRequest request) {
        var wearable = wearableDeviceService.findCurrentEntity();
        if (wearable == null) {
            throw new IllegalArgumentException("请先绑定可穿戴设备");
        }
        Instant recordedAt = request.recordedAt() == null ? Instant.now() : request.recordedAt();
        if (recordedAt.isAfter(Instant.now().plusSeconds(60))) {
            throw new IllegalArgumentException("心率记录时间不能晚于当前时间");
        }
        var reading = HeartRateReading.create(
            UserProfileService.CURRENT_USER_ID,
            wearable.getId(),
            request.bpm(),
            request.scene() == null ? "resting" : request.scene(),
            recordedAt
        );
        return HeartRateReadingResponse.from(heartRateReadingRepository.save(reading));
    }
}
