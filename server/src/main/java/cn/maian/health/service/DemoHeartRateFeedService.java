package cn.maian.health.service;

import cn.maian.health.domain.HeartRateReading;
import cn.maian.health.repository.HeartRateReadingRepository;
import cn.maian.health.repository.WearableDeviceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoHeartRateFeedService {

    private static final ZoneId USER_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Duration RECENT_SAMPLE_WINDOW = Duration.ofMinutes(4);
    private static final List<UUID> DEMO_USERS = List.of(
        UUID.fromString("30000000-0000-0000-0000-000000000001"),
        UUID.fromString("30000000-0000-0000-0000-000000000002"),
        UUID.fromString("30000000-0000-0000-0000-000000000003")
    );

    private final HeartRateReadingRepository readingRepository;
    private final WearableDeviceRepository wearableRepository;

    public DemoHeartRateFeedService(
        HeartRateReadingRepository readingRepository,
        WearableDeviceRepository wearableRepository
    ) {
        this.readingRepository = readingRepository;
        this.wearableRepository = wearableRepository;
    }

    @Transactional
    @Scheduled(initialDelay = 1500, fixedDelay = 300000)
    public void appendCurrentSamples() {
        Instant sampledAt = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        for (int index = 0; index < DEMO_USERS.size(); index++) {
            UUID userId = DEMO_USERS.get(index);
            var wearable = wearableRepository.findByUserId(userId)
                .filter(device -> device.isConnected());
            if (wearable.isEmpty() || hasRecentSample(userId, sampledAt)) continue;

            int hour = sampledAt.atZone(USER_ZONE).getHour();
            String scene = hour < 6 ? "sleeping" : hour >= 17 && hour < 19 ? "exercise" : "resting";
            int baseline = "sleeping".equals(scene) ? 61 : "exercise".equals(scene) ? 106 : 74;
            int variation = Math.floorMod(sampledAt.getEpochSecond() / 300 + index * 3, 13);
            readingRepository.save(HeartRateReading.create(
                userId,
                wearable.get().getId(),
                baseline + variation,
                scene,
                sampledAt
            ));
        }
    }

    private boolean hasRecentSample(UUID userId, Instant sampledAt) {
        return readingRepository.findFirstByUserIdOrderByRecordedAtDesc(userId)
            .map(reading -> !reading.getRecordedAt().isBefore(sampledAt.minus(RECENT_SAMPLE_WINDOW)))
            .orElse(false);
    }
}
