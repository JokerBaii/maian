package cn.maian.rescue.domain;

import cn.maian.device.domain.DeviceStatus;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RescueCallResponderFlowTest {

    @Test
    void fixedAedFollowsPickupCompletionConfirmationAndReturnWorkflow() {
        UUID requesterId = UUID.randomUUID();
        UUID responderId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-08T10:00:00Z");
        EmergencyDevice device = EmergencyDevice.create(
            DeviceType.FIXED, "AED", "测试 AED", "测试地址",
            120.1, 30.2, null, cn.maian.device.domain.DeviceServiceWindow.alwaysOpen(), null,
            "测试资源方", null, null, null, List.of(), List.of()
        );
        device.review(true, "资料完整");

        RescueCall call = RescueCall.create(
            UrgencyLevel.CRITICAL, 30.2, 120.1, "演示呼救点",
            "需要 AED", Set.of("意识丧失"), "demo-flow", requesterId, now
        );
        call.beginMatching(now);
        device.markReserved(call.getId(), now);
        call.assignDevice(device, now, 120, 90, "TEST_FIXED");
        call.acceptBy(responderId, now.plusSeconds(1));
        call.arriveAtAed(now.plusSeconds(20));
        call.pickUpAed(now.plusSeconds(25));
        call.arriveAtRequester(now.plusSeconds(80));
        call.startRescue(now.plusSeconds(81));
        call.submitCompletion(now.plusSeconds(180));
        call.confirmCompletion(now.plusSeconds(190));

        assertThat(call.getStatus()).isEqualTo(RescueStatus.COMPLETED);
        assertThat(call.getAedCustodyStatus()).isEqualTo(AedCustodyStatus.RETURNING);
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.RESERVED);

        call.returnAed(now.plusSeconds(240));

        assertThat(call.getAedCustodyStatus()).isEqualTo(AedCustodyStatus.RETURNED);
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.AVAILABLE);
        assertThat(call.getResponderUserId()).isEqualTo(responderId);
    }
}
