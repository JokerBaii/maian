package cn.maian.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private boolean rescuePush;

    @Column(nullable = false)
    private boolean healthAlert;

    @Column(nullable = false)
    private boolean scienceUpdate;

    @Column(nullable = false)
    private boolean locationShare;

    @Column(nullable = false)
    private boolean healthDataShare;

    @Column(nullable = false)
    private int maxHeartRate;

    @Column(nullable = false)
    private int minHeartRate;

    @Column(nullable = false)
    private Instant updatedAt;

    protected UserSettings() {
    }

    public static UserSettings defaults(UUID userId) {
        var settings = new UserSettings();
        settings.userId = userId;
        settings.rescuePush = true;
        settings.healthAlert = true;
        settings.locationShare = true;
        settings.maxHeartRate = 120;
        settings.minHeartRate = 50;
        settings.updatedAt = Instant.now();
        return settings;
    }

    public void update(
        boolean rescuePush,
        boolean healthAlert,
        boolean scienceUpdate,
        boolean locationShare,
        boolean healthDataShare,
        int maxHeartRate,
        int minHeartRate
    ) {
        this.rescuePush = rescuePush;
        this.healthAlert = healthAlert;
        this.scienceUpdate = scienceUpdate;
        this.locationShare = locationShare;
        this.healthDataShare = healthDataShare;
        this.maxHeartRate = maxHeartRate;
        this.minHeartRate = minHeartRate;
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public boolean isRescuePush() { return rescuePush; }
    public boolean isHealthAlert() { return healthAlert; }
    public boolean isScienceUpdate() { return scienceUpdate; }
    public boolean isLocationShare() { return locationShare; }
    public boolean isHealthDataShare() { return healthDataShare; }
    public int getMaxHeartRate() { return maxHeartRate; }
    public int getMinHeartRate() { return minHeartRate; }
    public Instant getUpdatedAt() { return updatedAt; }
}
