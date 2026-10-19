package cn.maian.agent.domain;

import cn.maian.device.domain.EmergencyDevice;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "coordination_tasks")
public class CoordinationTask {
    @Id @JdbcTypeCode(SqlTypes.CHAR) @Column(length = 36)
    private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(length = 36, nullable = false)
    private UUID ownerUserId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(length = 36, nullable = false)
    private UUID deviceId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(length = 36)
    private UUID rescueCallId;
    @Column(length = 120, nullable = false)
    private String deviceName;
    @Column(length = 40, nullable = false)
    private String triggerType;
    @Column(length = 160, nullable = false, unique = true)
    private String eventKey;
    @Column(length = 24, nullable = false)
    private String status;
    @Column(length = 24, nullable = false)
    private String executionMode;
    @Column(length = 600, nullable = false)
    private String summary;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant leaseUntil;
    private Instant completedAt;
    private boolean actionsApplied;
    private boolean attentionRequired;
    @Version private long version;
    @ElementCollection
    @CollectionTable(name = "coordination_steps", joinColumns = @JoinColumn(name = "task_id"))
    @OrderColumn(name = "position")
    private List<CoordinationStep> steps = new ArrayList<>();

    protected CoordinationTask() {}
    public static CoordinationTask create(EmergencyDevice device, String trigger, String key, Instant now) {
        var task = new CoordinationTask();
        task.id = UUID.randomUUID();
        task.ownerUserId = device.getRegisteredByUserId();
        task.deviceId = device.getId();
        task.rescueCallId = device.getReservedForCallId();
        task.deviceName = device.getName();
        task.triggerType = trigger;
        task.eventKey = key;
        task.status = "PENDING";
        task.executionMode = "PENDING";
        task.summary = "已收到设备变化，等待检查资源与救援任务";
        task.createdAt = now;
        task.updatedAt = now;
        task.addStep("设备管理", "收到设备事件：" + triggerLabel(trigger), now);
        return task;
    }
    public boolean claim(Instant now) {
        if (!"PENDING".equals(status) && !("RUNNING".equals(status) && !leaseUntil.isAfter(now))) return false;
        if ("RUNNING".equals(status)) addStep("任务协调", "上次执行中断，正在恢复任务", now);
        status = "RUNNING";
        leaseUntil = now.plusSeconds(180);
        updatedAt = now;
        return true;
    }
    public void addStep(String module, String detail, Instant now) {
        // Bound stored tool traces even if a model repeats calls.
        if (steps.size() < 24) steps.add(new CoordinationStep(module, detail, now));
        updatedAt = now;
    }
    public void applied(String result, boolean attention, Instant now) {
        actionsApplied = true;
        attentionRequired = attention;
        summary = result;
        updatedAt = now;
    }
    public void complete(String mode, Instant now) {
        executionMode = mode;
        status = attentionRequired ? "ATTENTION" : "COMPLETED";
        completedAt = now;
        updatedAt = now;
    }
    public void fail(Instant now) {
        status = "FAILED";
        if (!actionsApplied) summary = "协同处理未完成，请检查设备与相关任务后重试";
        completedAt = now;
        updatedAt = now;
    }
    public void retry(Instant now) {
        if (!"FAILED".equals(status)) throw new IllegalArgumentException("只有处理失败的任务可以重试");
        status = "PENDING";
        completedAt = null;
        updatedAt = now;
        addStep("任务协调", "设备管理方请求重新检查", now);
    }
    public void resolveRepair(Instant now) {
        resolve("设备管理方已确认修复并恢复，已保留原救援的设备安排", "人工核实", now);
    }
    public void resolve(String result, String module, Instant now) {
        if (!needsFollowUp()) return;
        attentionRequired = false;
        summary = result;
        status = "COMPLETED";
        completedAt = now;
        addStep(module, summary, now);
    }
    public boolean needsFollowUp() { return "ATTENTION".equals(status); }
    public void checked(Instant now) { updatedAt = now; }
    public static String triggerLabel(String trigger) {
        return switch (trigger) {
            case "DISABLED" -> "设备停用维护";
            case "FAULT_REPORTED" -> "设备故障上报";
            case "ENABLED" -> "设备重新启用";
            case "APPROVED" -> "设备审核通过";
            case "UPDATED" -> "设备资料更新";
            case "DELETED" -> "设备已删除";
            case "LOCATION_RESTORED" -> "移动设备位置恢复";
            case "UNAVAILABLE" -> "设备已不满足调度条件";
            default -> "设备状态变化";
        };
    }
    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public UUID getDeviceId() { return deviceId; }
    public UUID getRescueCallId() { return rescueCallId; }
    public String getDeviceName() { return deviceName; }
    public String getTriggerType() { return triggerType; }
    public String getStatus() { return status; }
    public String getExecutionMode() { return executionMode; }
    public String getSummary() { return summary; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public boolean isActionsApplied() { return actionsApplied; }
    public List<CoordinationStep> getSteps() { return List.copyOf(steps); }
}
