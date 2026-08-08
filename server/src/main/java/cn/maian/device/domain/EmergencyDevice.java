package cn.maian.device.domain;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "emergency_devices")
public class EmergencyDevice {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private DeviceType type;

    @Column(length = 30, nullable = false)
    private String category;

    @Column(length = 120, nullable = false)
    private String name;

    @Column(length = 255, nullable = false)
    private String address;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private double latitude;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private DeviceStatus status;

    @Column(length = 40)
    private String ownerPhone;

    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "emergency_device_service_windows", joinColumns = @JoinColumn(name = "device_id"))
    @OrderColumn(name = "position")
    private List<DeviceServiceWindow> serviceWindows = new ArrayList<>();

    private LocalDate expireDate;

    @Column(length = 120)
    private String owner;

    @Column(length = 120)
    private String vehicleInfo;

    private Integer serviceRange;

    @Column(length = 500)
    private String instructions;

    private Instant lastLocationAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID reservedForCallId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID registeredByUserId;

    private Instant reservedAt;

    @Column(length = 300)
    private String reviewNote;

    private Instant reviewedAt;

    @ElementCollection
    @CollectionTable(
        name = "emergency_device_images",
        joinColumns = @JoinColumn(name = "device_id")
    )
    @OrderColumn(name = "position")
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "media_id", length = 36, nullable = false)
    private List<UUID> imageMediaIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
        name = "emergency_device_vehicle_images",
        joinColumns = @JoinColumn(name = "device_id")
    )
    @OrderColumn(name = "position")
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "media_id", length = 36, nullable = false)
    private List<UUID> vehicleImageMediaIds = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected EmergencyDevice() {
    }

    public static EmergencyDevice create(
        DeviceType type,
        String category,
        String name,
        String address,
        double longitude,
        double latitude,
        String ownerPhone,
        List<DeviceServiceWindow> serviceWindows,
        LocalDate expireDate,
        String owner,
        String vehicleInfo,
        Integer serviceRange,
        String instructions,
        List<UUID> imageMediaIds,
        List<UUID> vehicleImageMediaIds
    ) {
        var device = new EmergencyDevice();
        device.id = UUID.randomUUID();
        device.status = DeviceStatus.PENDING_REVIEW;
        device.createdAt = Instant.now();
        device.update(type, category, name, address, longitude, latitude, ownerPhone, serviceWindows,
            expireDate, owner, vehicleInfo, serviceRange, instructions, imageMediaIds, vehicleImageMediaIds);
        return device;
    }

    public void update(
        DeviceType type,
        String category,
        String name,
        String address,
        double longitude,
        double latitude,
        String ownerPhone,
        List<DeviceServiceWindow> serviceWindows,
        LocalDate expireDate,
        String owner,
        String vehicleInfo,
        Integer serviceRange,
        String instructions,
        List<UUID> imageMediaIds,
        List<UUID> vehicleImageMediaIds
    ) {
        DeviceType previousType = this.type;
        boolean criticalChanged = this.type != null && (
            this.type != type
                || !Objects.equals(this.category, category)
                || !Objects.equals(this.address, address)
                || Double.compare(this.longitude, longitude) != 0
                || Double.compare(this.latitude, latitude) != 0
                || !Objects.equals(this.serviceWindows, serviceWindows)
                || !Objects.equals(this.expireDate, expireDate)
                || !Objects.equals(this.vehicleInfo, vehicleInfo)
                || !Objects.equals(this.serviceRange, serviceRange)
                || !Objects.equals(this.imageMediaIds, imageMediaIds)
                || !Objects.equals(this.vehicleImageMediaIds, vehicleImageMediaIds)
        );
        this.type = type;
        this.category = category;
        this.name = name;
        this.address = address;
        this.longitude = longitude;
        this.latitude = latitude;
        this.ownerPhone = ownerPhone;
        this.serviceWindows = new ArrayList<>(serviceWindows);
        this.expireDate = expireDate;
        this.owner = owner;
        this.vehicleInfo = vehicleInfo;
        this.serviceRange = serviceRange;
        this.instructions = instructions;
        this.imageMediaIds = new ArrayList<>(imageMediaIds);
        this.vehicleImageMediaIds = new ArrayList<>(vehicleImageMediaIds);
        if (criticalChanged && this.status != DeviceStatus.PENDING_REVIEW) {
            this.status = DeviceStatus.PENDING_REVIEW;
            this.reviewNote = null;
            this.reviewedAt = null;
        }
        if (type == DeviceType.MOBILE && previousType != DeviceType.MOBILE) {
            this.lastLocationAt = null;
        } else if (type == DeviceType.FIXED) {
            this.lastLocationAt = null;
        }
    }

    public void enable() {
        if (status != DeviceStatus.DISABLED) {
            throw invalid("只有已停用设备可以启用");
        }
        if (reviewedAt == null) {
            throw invalid("设备通过审核后才能启用");
        }
        status = DeviceStatus.AVAILABLE;
    }

    public void disable() {
        if (status != DeviceStatus.AVAILABLE) {
            throw invalid("只有可用设备可以停用");
        }
        status = DeviceStatus.DISABLED;
    }

    public void review(boolean approved, String note) {
        if (status != DeviceStatus.PENDING_REVIEW && status != DeviceStatus.REJECTED) {
            throw invalid("只有待审核或已驳回设备可以审核");
        }
        this.status = approved ? DeviceStatus.AVAILABLE : DeviceStatus.REJECTED;
        this.reviewNote = note == null || note.isBlank() ? null : note.trim();
        this.reviewedAt = Instant.now();
    }

    public void registerTo(UUID userId) {
        if (this.registeredByUserId != null && !this.registeredByUserId.equals(userId)) {
            throw new IllegalStateException("设备已绑定其他用户");
        }
        this.registeredByUserId = userId;
    }

    public void updateLocation(double longitude, double latitude, String address, Instant updatedAt) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.address = address;
        this.lastLocationAt = updatedAt;
    }

    public void releaseReservation(UUID rescueCallId) {
        if (!rescueCallId.equals(this.reservedForCallId)) {
            return;
        }
        this.status = DeviceStatus.AVAILABLE;
        this.reservedForCallId = null;
        this.reservedAt = null;
    }

    public void markReserved(UUID rescueCallId, Instant at) {
        if (status != DeviceStatus.AVAILABLE || reservedForCallId != null) {
            throw invalid("设备当前不可预约");
        }
        status = DeviceStatus.RESERVED;
        reservedForCallId = rescueCallId;
        reservedAt = at;
    }

    private cn.maian.common.exception.InvalidStateTransitionException invalid(String message) {
        return new cn.maian.common.exception.InvalidStateTransitionException(message);
    }

    public UUID getId() {
        return id;
    }

    public DeviceType getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public String getOwnerPhone() {
        return ownerPhone;
    }

    public List<DeviceServiceWindow> getServiceWindows() {
        return List.copyOf(serviceWindows);
    }

    public boolean isWithinServiceWindow(DayOfWeek dayOfWeek, LocalTime time) {
        return serviceWindows.stream().anyMatch(window -> window.contains(dayOfWeek, time));
    }

    public MobilePresenceStatus mobilePresenceAt(Instant now, long freshSeconds) {
        if (type != DeviceType.MOBILE) {
            return null;
        }
        if (status == DeviceStatus.DISABLED || lastLocationAt == null) {
            return MobilePresenceStatus.OFFLINE;
        }
        long ageSeconds = Math.max(0, Duration.between(lastLocationAt, now).toSeconds());
        if (ageSeconds <= freshSeconds) {
            return MobilePresenceStatus.ONLINE;
        }
        return ageSeconds <= freshSeconds * 3
            ? MobilePresenceStatus.STALE
            : MobilePresenceStatus.OFFLINE;
    }

    public LocalDate getExpireDate() {
        return expireDate;
    }

    public String getOwner() {
        return owner;
    }

    public String getVehicleInfo() {
        return vehicleInfo;
    }

    public Integer getServiceRange() {
        return serviceRange;
    }

    public String getInstructions() {
        return instructions;
    }

    public List<UUID> getImageMediaIds() {
        return List.copyOf(imageMediaIds);
    }

    public List<UUID> getVehicleImageMediaIds() {
        return List.copyOf(vehicleImageMediaIds);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLocationAt() {
        return lastLocationAt;
    }

    public UUID getReservedForCallId() {
        return reservedForCallId;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public long getVersion() {
        return version;
    }

    public UUID getRegisteredByUserId() {
        return registeredByUserId;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
