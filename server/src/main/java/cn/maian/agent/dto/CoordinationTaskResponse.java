package cn.maian.agent.dto;

import cn.maian.agent.domain.CoordinationTask;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoordinationTaskResponse(
    UUID id, UUID deviceId, String deviceName, String triggerLabel, String status,
    String executionMode, String summary, Instant createdAt, Instant completedAt, List<Step> steps
) {
    public record Step(String module, String summary, Instant occurredAt) {}
    public static CoordinationTaskResponse from(CoordinationTask task) {
        return new CoordinationTaskResponse(task.getId(), task.getDeviceId(), task.getDeviceName(),
            CoordinationTask.triggerLabel(task.getTriggerType()), task.getStatus(), task.getExecutionMode(),
            task.getSummary(), task.getCreatedAt(), task.getCompletedAt(), task.getSteps().stream()
                .map(step -> new Step(step.getModule(), step.getSummary(), step.getOccurredAt())).toList());
    }
}
