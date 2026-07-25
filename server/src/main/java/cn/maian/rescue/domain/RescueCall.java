package cn.maian.rescue.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "rescue_calls")
public class RescueCall {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UrgencyLevel urgency;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private RescueStatus status;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(length = 255, nullable = false)
    private String address;

    @Column(length = 1000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "rescue_call_symptoms",
        joinColumns = @JoinColumn(name = "rescue_call_id")
    )
    @Column(name = "symptom", length = 50, nullable = false)
    private Set<String> symptoms = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "rescue_call_images",
        joinColumns = @JoinColumn(name = "rescue_call_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "image_url", length = 500, nullable = false)
    private List<String> imageUrls = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected RescueCall() {
    }

    private RescueCall(
        UUID id,
        UrgencyLevel urgency,
        double latitude,
        double longitude,
        String address,
        String description,
        Set<String> symptoms,
        List<String> imageUrls
    ) {
        this.id = id;
        this.urgency = urgency;
        this.status = RescueStatus.PENDING;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.description = description;
        this.symptoms = new LinkedHashSet<>(symptoms);
        this.imageUrls = new ArrayList<>(imageUrls);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static RescueCall create(
        UrgencyLevel urgency,
        double latitude,
        double longitude,
        String address,
        String description,
        Set<String> symptoms,
        List<String> imageUrls
    ) {
        return new RescueCall(
            UUID.randomUUID(),
            urgency,
            latitude,
            longitude,
            address,
            description,
            symptoms,
            imageUrls
        );
    }

    public void transitionTo(RescueStatus nextStatus) {
        this.status = nextStatus;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UrgencyLevel getUrgency() {
        return urgency;
    }

    public RescueStatus getStatus() {
        return status;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getAddress() {
        return address;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getSymptoms() {
        return Set.copyOf(symptoms);
    }

    public List<String> getImageUrls() {
        return List.copyOf(imageUrls);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
