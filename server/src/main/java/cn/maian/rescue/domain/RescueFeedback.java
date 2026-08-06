package cn.maian.rescue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rescue_feedback")
public class RescueFeedback {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "rescue_call_id", length = 36, nullable = false, unique = true)
    private UUID rescueCallId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "from_user_id", length = 36, nullable = false)
    private UUID fromUserId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "to_user_id", length = 36)
    private UUID toUserId;

    @Column(nullable = false)
    private int rating;

    @Column(length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RescueFeedback() {
    }

    public RescueFeedback(UUID id, UUID rescueCallId, UUID fromUserId, UUID toUserId, int rating, String comment) {
        this.id = id;
        this.rescueCallId = rescueCallId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.rating = rating;
        this.comment = comment == null || comment.isBlank() ? null : comment.trim();
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getRescueCallId() {
        return rescueCallId;
    }

    public UUID getFromUserId() {
        return fromUserId;
    }

    public UUID getToUserId() {
        return toUserId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
