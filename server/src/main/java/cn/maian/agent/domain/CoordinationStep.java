package cn.maian.agent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class CoordinationStep {
    @Column(name = "module_name", length = 40, nullable = false)
    private String module;
    @Column(length = 600, nullable = false)
    private String summary;
    @Column(nullable = false)
    private Instant occurredAt;

    protected CoordinationStep() {}
    public CoordinationStep(String module, String summary, Instant at) {
        this.module = module;
        this.summary = summary;
        this.occurredAt = at;
    }
    public String getModule() { return module; }
    public String getSummary() { return summary; }
    public Instant getOccurredAt() { return occurredAt; }
}
