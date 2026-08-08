package cn.maian.rescue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "rescue_events",
    uniqueConstraints = @UniqueConstraint(name = "uk_rescue_event_sequence", columnNames = {"rescue_call_id", "sequence_no"})
)
public class RescueEvent {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "rescue_call_id", length = 36, nullable = false)
    private UUID rescueCallId;

    @Column(name = "sequence_no", nullable = false)
    private long sequence;

    @Enumerated(EnumType.STRING)
    @Column(length = 40, nullable = false)
    private RescueEventType type;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID actorUserId;

    @Column(length = 300)
    private String summary;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected RescueEvent() {
    }

    public RescueEvent(
        UUID rescueCallId,
        long sequence,
        RescueEventType type,
        UUID actorUserId,
        String summary,
        Instant createdAt
    ) {
        this.id = UUID.randomUUID();
        this.rescueCallId = rescueCallId;
        this.sequence = sequence;
        this.type = type;
        this.actorUserId = actorUserId;
        this.summary = summary;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getRescueCallId() { return rescueCallId; }
    public long getSequence() { return sequence; }
    public RescueEventType getType() { return type; }
    public UUID getActorUserId() { return actorUserId; }
    public String getSummary() { return summary; }
    public Instant getCreatedAt() { return createdAt; }
}
