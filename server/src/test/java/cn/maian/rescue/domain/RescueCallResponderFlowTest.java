package cn.maian.rescue.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RescueCallResponderFlowTest {

    @Test
    void responderCanAcceptStartAndCompleteTask() {
        var rescueCall = RescueCall.create(
            UrgencyLevel.CRITICAL, 30.2, 120.1, "演示呼救点",
            "需要 AED", Set.of("意识丧失"), List.of(), "demo-flow"
        );
        UUID responderId = UUID.randomUUID();
        rescueCall.beginMatching();

        rescueCall.acceptBy(responderId);
        rescueCall.transitionTo(RescueStatus.RESCUING);
        rescueCall.transitionTo(RescueStatus.COMPLETED);

        assertThat(rescueCall.getResponderUserId()).isEqualTo(responderId);
        assertThat(rescueCall.getAcceptedAt()).isNotNull();
        assertThat(rescueCall.getCompletedAt()).isNotNull();
        assertThat(rescueCall.getStatus()).isEqualTo(RescueStatus.COMPLETED);
    }
}
