package cn.maian.device.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmergencyDeviceReviewTest {

    @Test
    void newlySubmittedDeviceRequiresReviewBeforeBecomingAvailable() {
        var device = EmergencyDevice.create(
            DeviceType.FIXED, "AED", "演示 AED", "演示地址",
            120.1, 30.2, null, DeviceServiceWindow.alwaysOpen(), null,
            "演示资源方", null, null, null, List.of(), List.of()
        );

        assertThat(device.getStatus()).isEqualTo(DeviceStatus.PENDING_REVIEW);

        device.review(true, "资料完整");

        assertThat(device.getStatus()).isEqualTo(DeviceStatus.AVAILABLE);
        assertThat(device.getReviewedAt()).isNotNull();
    }
}
