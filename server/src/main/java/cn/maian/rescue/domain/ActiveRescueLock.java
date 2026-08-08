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
@Table(name = "active_rescue_locks")
public class ActiveRescueLock {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false, unique = true)
    private UUID rescueCallId;

    @Column(nullable = false, updatable = false)
    private Instant acquiredAt;

    protected ActiveRescueLock() {
    }

    public ActiveRescueLock(UUID userId, UUID rescueCallId, Instant acquiredAt) {
        this.userId = userId;
        this.rescueCallId = rescueCallId;
        this.acquiredAt = acquiredAt;
    }

    public UUID getUserId() { return userId; }
    public UUID getRescueCallId() { return rescueCallId; }
    public Instant getAcquiredAt() { return acquiredAt; }
}
