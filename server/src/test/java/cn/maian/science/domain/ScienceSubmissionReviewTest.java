package cn.maian.science.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScienceSubmissionReviewTest {

    @Test
    void shouldApprovePendingSubmissionOnlyOnce() {
        var submission = ScienceSubmission.create(
            UUID.randomUUID(), "AED 使用要点", "device", "请遵循设备语音提示。", null
        );

        submission.review(true, "内容准确");

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(submission.getReviewNote()).isEqualTo("内容准确");
        assertThat(submission.getReviewedAt()).isNotNull();
        assertThatThrownBy(() -> submission.review(false, "重复审核"))
            .isInstanceOf(IllegalStateException.class);
    }
}
