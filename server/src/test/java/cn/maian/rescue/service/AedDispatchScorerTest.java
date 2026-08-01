package cn.maian.rescue.service;

import cn.maian.config.DispatchProperties;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class AedDispatchScorerTest {

    private static final DispatchProperties PROPERTIES = new DispatchProperties(
        15,
        80,
        120,
        35,
        6.5,
        1.25,
        30,
        20,
        900
    );

    @Test
    void calculatesKnownDistanceWithinSmallErrorMargin() {
        double meters = AedDispatchScorer.haversineMeters(
            30.2800,
            120.1500,
            30.2890,
            120.1500
        );

        assertThat(meters).isBetween(995.0, 1_007.0);
    }

    @Test
    void mobileAedCanBeatCloserFixedAedByDeliveryTime() {
        Instant now = Instant.now();
        EmergencyDevice mobile = device(DeviceType.MOBILE, 30.2890, 120.1500, 5);
        EmergencyDevice fixed = device(DeviceType.FIXED, 30.2827, 120.1500, null);

        var mobileScore = AedDispatchScorer.score(30.2800, 120.1500, mobile, now, PROPERTIES);
        var fixedScore = AedDispatchScorer.score(30.2800, 120.1500, fixed, now, PROPERTIES);

        assertThat(mobileScore.eligible()).isTrue();
        assertThat(fixedScore.eligible()).isTrue();
        assertThat(mobileScore.distanceMeters()).isGreaterThan(fixedScore.distanceMeters());
        assertThat(mobileScore.estimatedArrivalSeconds())
            .isLessThan(fixedScore.estimatedArrivalSeconds());
    }

    @Test
    void excludesMobileAedOutsideOwnerServiceRange() {
        EmergencyDevice mobile = device(DeviceType.MOBILE, 30.2980, 120.1500, 1);

        var score = AedDispatchScorer.score(
            30.2800,
            120.1500,
            mobile,
            Instant.now(),
            PROPERTIES
        );

        assertThat(score.eligible()).isFalse();
    }

    @Test
    void scoresLargeCandidateBatchesWithoutNativeCode() {
        EmergencyDevice mobile = device(DeviceType.MOBILE, 30.2890, 120.1500, 15);
        Instant now = Instant.now();

        assertTimeout(Duration.ofSeconds(2), () -> {
            long checksum = 0;
            for (int index = 0; index < 250_000; index++) {
                var score = AedDispatchScorer.score(
                    30.28 + (index % 7) * 0.00001,
                    120.15,
                    mobile,
                    now,
                    PROPERTIES
                );
                checksum += score.estimatedArrivalSeconds();
            }
            assertThat(checksum).isPositive();
        });
    }

    private static EmergencyDevice device(
        DeviceType type,
        double latitude,
        double longitude,
        Integer serviceRange
    ) {
        return EmergencyDevice.create(
            type,
            "AED",
            type + " AED",
            "测试位置",
            longitude,
            latitude,
            "13500000000",
            "全天",
            null,
            "测试资源方",
            type == DeviceType.MOBILE ? "测试车辆" : null,
            serviceRange,
            null,
            List.of(),
            List.of()
        );
    }
}
