package cn.maian.agent.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RescueReviewResponse(UUID rescueCallId, String status, String executionMode, String summary,
    boolean stale, Instant updatedAt, Instant completedAt, List<CoordinationTaskResponse.Step> steps,
    Data data, List<Article> articles) {
    public record Metric(String label, Long seconds) {}
    public record Event(String type, String summary, Instant occurredAt) {}
    public record Data(List<Metric> metrics, List<Event> timeline, boolean returnPending, boolean feedbackPending,
        String custodyStatus, List<String> articleIds) {}
    public record Article(String id, String title, String summary) {}
}
