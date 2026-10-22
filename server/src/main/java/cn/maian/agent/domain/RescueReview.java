package cn.maian.agent.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rescue_reviews")
public class RescueReview {
    @Id @JdbcTypeCode(SqlTypes.CHAR) @Column(length = 36)
    private UUID rescueCallId;
    @Column(length = 24, nullable = false) private String status;
    @Column(length = 24, nullable = false) private String executionMode;
    @Column(length = 600, nullable = false) private String summary;
    private boolean useModel;
    private long sourceSequence;
    private boolean feedbackPresent;
    @Column(columnDefinition = "LONGTEXT") private String resultJson;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(length = 36) private UUID executionToken;
    private Instant leaseUntil;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    @Version private long version;
    @ElementCollection
    @CollectionTable(name = "rescue_review_steps", joinColumns = @JoinColumn(name = "rescue_call_id"))
    @OrderColumn(name = "position")
    private List<CoordinationStep> steps = new ArrayList<>();

    protected RescueReview() {}
    public static RescueReview create(UUID callId, boolean useModel, Instant now) {
        var review = new RescueReview();
        review.rescueCallId = callId;
        review.createdAt = now;
        review.request(useModel, now);
        return review;
    }
    public void request(boolean useModel, Instant now) {
        this.useModel = useModel;
        status = "PENDING";
        executionMode = "PENDING";
        summary = "已收到复盘请求，等待核对救援流程与后续待办";
        resultJson = null;
        completedAt = null;
        executionToken = null;
        leaseUntil = null;
        steps.clear();
        addStep("用户请求", "请求整理已结束救援的流程事实、业务待办与学习资料", now);
    }
    public boolean inProgress(Instant now) {
        return "PENDING".equals(status) || ("RUNNING".equals(status) && leaseUntil.isAfter(now));
    }
    public UUID claim(Instant now) {
        if (!"PENDING".equals(status) && !("RUNNING".equals(status) && !leaseUntil.isAfter(now))) return null;
        if ("RUNNING".equals(status)) addStep("任务恢复", "执行租约已过期，重新核对当前事实", now);
        status = "RUNNING";
        executionToken = UUID.randomUUID();
        leaseUntil = now.plusSeconds(180);
        updatedAt = now;
        return executionToken;
    }
    public void requireExecution(UUID token, Instant now) {
        if (!ownsExecution(token) || !leaseUntil.isAfter(now)) throw new IllegalStateException("复盘执行窗口已结束");
    }
    private boolean ownsExecution(UUID token) {
        return "RUNNING".equals(status) && token != null && token.equals(executionToken);
    }
    public void addStep(String module, String detail, Instant now) {
        if (steps.size() < 24) steps.add(new CoordinationStep(module, detail, now));
        updatedAt = now;
    }
    public void record(String json, long sequence, boolean feedbackPresent, String summary, Instant now) {
        this.resultJson = json;
        this.sourceSequence = sequence;
        this.feedbackPresent = feedbackPresent;
        this.summary = summary;
        addStep("复盘整理", summary, now);
    }
    public void complete(String mode, Instant now) {
        if (resultJson == null) throw new IllegalStateException("尚未整理复盘结果");
        status = "COMPLETED";
        executionMode = mode;
        completedAt = now;
        updatedAt = now;
    }
    public void fail(UUID token, Instant now) {
        if (!ownsExecution(token)) return;
        status = "FAILED";
        summary = "复盘整理未完成，可重新尝试";
        updatedAt = now;
        completedAt = now;
    }
    public UUID getRescueCallId() { return rescueCallId; }
    public String getStatus() { return status; }
    public String getExecutionMode() { return executionMode; }
    public String getSummary() { return summary; }
    public boolean isUseModel() { return useModel; }
    public long getSourceSequence() { return sourceSequence; }
    public boolean isFeedbackPresent() { return feedbackPresent; }
    public String getResultJson() { return resultJson; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<CoordinationStep> getSteps() { return List.copyOf(steps); }
}
