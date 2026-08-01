package cn.maian.health.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "heart_rate_readings")
public class HeartRateReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID wearableId;

    @Column(nullable = false)
    private int bpm;

    @Column(length = 20, nullable = false)
    private String scene;

    @Column(nullable = false)
    private Instant recordedAt;

    protected HeartRateReading() {
    }

    public static HeartRateReading create(
        UUID userId,
        UUID wearableId,
        int bpm,
        String scene,
        Instant recordedAt
    ) {
        var reading = new HeartRateReading();
        reading.userId = userId;
        reading.wearableId = wearableId;
        reading.bpm = bpm;
        reading.scene = scene;
        reading.recordedAt = recordedAt;
        return reading;
    }

    public Long getId() { return id; }
    public int getBpm() { return bpm; }
    public String getScene() { return scene; }
    public Instant getRecordedAt() { return recordedAt; }
}
