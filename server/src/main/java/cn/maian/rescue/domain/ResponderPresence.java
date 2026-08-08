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
@Table(name = "responder_presence")
public class ResponderPresence {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private boolean available;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ResponderPresence() {
    }

    public ResponderPresence(UUID userId, double latitude, double longitude, boolean available, Instant now) {
        this.userId = userId;
        update(latitude, longitude, available, now);
    }

    public void update(double latitude, double longitude, boolean available, Instant now) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = available;
        this.updatedAt = now;
    }

    public boolean isEligibleAt(Instant freshSince) {
        return available && !updatedAt.isBefore(freshSince);
    }

    public UUID getUserId() { return userId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isAvailable() { return available; }
    public Instant getUpdatedAt() { return updatedAt; }
}
