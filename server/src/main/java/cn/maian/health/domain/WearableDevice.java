package cn.maian.health.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wearable_devices")
public class WearableDevice {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID userId;

    @Column(length = 160, nullable = false)
    private String deviceIdentifier;

    @Column(length = 120, nullable = false)
    private String name;

    @Column(length = 40, nullable = false)
    private String type;

    @Column(nullable = false)
    private boolean connected;

    private Integer battery;

    private Instant lastSeenAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected WearableDevice() {
    }

    public static WearableDevice bind(
        UUID userId,
        String deviceIdentifier,
        String name,
        String type,
        boolean connected,
        Integer battery
    ) {
        var device = new WearableDevice();
        device.id = UUID.randomUUID();
        device.userId = userId;
        device.createdAt = Instant.now();
        device.update(deviceIdentifier, name, type, connected, battery);
        return device;
    }

    public void update(
        String deviceIdentifier,
        String name,
        String type,
        boolean connected,
        Integer battery
    ) {
        this.deviceIdentifier = deviceIdentifier.trim();
        this.name = name.trim();
        this.type = type.trim();
        this.connected = connected;
        this.battery = battery;
        this.lastSeenAt = connected ? Instant.now() : this.lastSeenAt;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getDeviceIdentifier() { return deviceIdentifier; }
    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isConnected() { return connected; }
    public Integer getBattery() { return battery; }
    public Instant getLastSeenAt() { return lastSeenAt; }
}
