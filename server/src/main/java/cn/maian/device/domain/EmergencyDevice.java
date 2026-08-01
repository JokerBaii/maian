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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(length = 80)
    private String serviceTime;

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

    @ElementCollection
    @CollectionTable(
        name = "emergency_device_images",
        joinColumns = @JoinColumn(name = "device_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "image_url", length = 500, nullable = false)
    private List<String> imageUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
        name = "emergency_device_vehicle_images",
        joinColumns = @JoinColumn(name = "device_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "image_url", length = 500, nullable = false)
    private List<String> vehicleImageUrls = new ArrayList<>();

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
        String serviceTime,
        LocalDate expireDate,
        String owner,
        String vehicleInfo,
        Integer serviceRange,
        String instructions,
        List<String> imageUrls,
        List<String> vehicleImageUrls
    ) {
        var device = new EmergencyDevice();
        device.id = UUID.randomUUID();
        device.status = DeviceStatus.AVAILABLE;
        device.createdAt = Instant.now();
        device.update(type, category, name, address, longitude, latitude, ownerPhone, serviceTime,
            expireDate, owner, vehicleInfo, serviceRange, instructions, imageUrls, vehicleImageUrls);
        if (type == DeviceType.MOBILE) {
            device.lastLocationAt = device.createdAt;
        }
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
        String serviceTime,
        LocalDate expireDate,
        String owner,
        String vehicleInfo,
        Integer serviceRange,
        String instructions,
        List<String> imageUrls,
        List<String> vehicleImageUrls
    ) {
        DeviceType previousType = this.type;
        this.type = type;
        this.category = category;
        this.name = name;
        this.address = address;
        this.longitude = longitude;
        this.latitude = latitude;
        this.ownerPhone = ownerPhone;
        this.serviceTime = serviceTime;
        this.expireDate = expireDate;
        this.owner = owner;
        this.vehicleInfo = vehicleInfo;
        this.serviceRange = serviceRange;
        this.instructions = instructions;
        this.imageUrls = new ArrayList<>(imageUrls);
        this.vehicleImageUrls = new ArrayList<>(vehicleImageUrls);
        if (type == DeviceType.MOBILE && previousType != DeviceType.MOBILE) {
            this.lastLocationAt = Instant.now();
        } else if (type == DeviceType.FIXED) {
            this.lastLocationAt = null;
        }
    }

    public void changeStatus(DeviceStatus status) {
        this.status = status;
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

    public String getServiceTime() {
        return serviceTime;
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

    public List<String> getImageUrls() {
        return List.copyOf(imageUrls);
    }

    public List<String> getVehicleImageUrls() {
        return List.copyOf(vehicleImageUrls);
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
}
