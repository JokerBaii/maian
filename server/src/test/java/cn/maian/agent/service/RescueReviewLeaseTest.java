package cn.maian.agent.service;

import cn.maian.agent.domain.RescueReview;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class RescueReviewLeaseTest {
    @Test
    void recoveredLeaseRejectsOldWorkerWritesAndFailure() {
        var now = Instant.now();
        var review = RescueReview.create(UUID.randomUUID(), false, now);
        var original = review.claim(now);
        assertThat(review.claim(now.plusSeconds(1))).isNull();
        var recovered = review.claim(now.plusSeconds(181));
        assertThat(recovered).isNotEqualTo(original);
        assertThatThrownBy(() -> review.requireExecution(original, now.plusSeconds(182))).isInstanceOf(IllegalStateException.class);
        review.fail(original, now.plusSeconds(182));
        assertThat(review.getStatus()).isEqualTo("RUNNING");
        review.requireExecution(recovered, now.plusSeconds(182));
    }

    @Test
    void completedOrRestartedReviewCannotBeOverwrittenByItsOldWorker() {
        var now = Instant.now();
        var review = RescueReview.create(UUID.randomUUID(), false, now);
        var token = review.claim(now);
        review.record("{}", 1, false, "完成", now);
        review.complete("RULES", now);
        review.fail(token, now);
        assertThat(review.getStatus()).isEqualTo("COMPLETED");
        review.request(true, now.plusSeconds(1));
        assertThatThrownBy(() -> review.requireExecution(token, now.plusSeconds(1))).isInstanceOf(IllegalStateException.class);
        assertThat(review.getStatus()).isEqualTo("PENDING");
    }
}
