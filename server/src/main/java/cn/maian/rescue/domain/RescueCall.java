package cn.maian.rescue.domain;

import cn.maian.common.exception.InvalidStateTransitionException;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "rescue_calls")
public class RescueCall {

    private static final Duration MATCH_WINDOW = Duration.ofMinutes(10);
    private static final Duration CONFIRMATION_WINDOW = Duration.ofMinutes(10);

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UrgencyLevel urgency;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
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
    @CollectionTable(name = "rescue_call_symptoms", joinColumns = @JoinColumn(name = "rescue_call_id"))
    @Column(name = "symptom", length = 50, nullable = false)
    private Set<String> symptoms = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "rescue_call_media", joinColumns = @JoinColumn(name = "rescue_call_id"))
    @OrderColumn(name = "position")
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "media_id", length = 36, nullable = false)
    private List<UUID> attachmentMediaIds = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant matchDeadlineAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_device_id")
    private EmergencyDevice matchedDevice;

    private Instant matchedAt;
    private Integer matchedDistanceMeters;
    private Integer estimatedArrivalSeconds;
    private Double matchedSnapshotLatitude;
    private Double matchedSnapshotLongitude;

    @Column(length = 255)
    private String matchedSnapshotAddress;

    @Column(length = 40)
    private String matchStrategy;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private AedCustodyStatus aedCustodyStatus;

    @Column(length = 64)
    private String clientRequestId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID requestedByUserId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID responderUserId;

    private Instant acceptedAt;
    private Instant arrivedAtAedAt;
    private Instant arrivedAt;
    private Instant rescueStartedAt;
    private Instant completionSubmittedAt;
    private Instant confirmationDeadlineAt;
    private Instant completedAt;
    private Instant aedReturnedAt;

    private Double responderLatitude;
    private Double responderLongitude;
    private Instant responderLocationAt;

    @Column(nullable = false)
    private long eventSequence;

    @Version
    @Column(nullable = false)
    private long version;

    protected RescueCall() {
    }

    public static RescueCall create(
        UrgencyLevel urgency,
        double latitude,
        double longitude,
        String address,
        String description,
        Set<String> symptoms,
        String clientRequestId,
        UUID requestedByUserId,
        Instant now
    ) {
        RescueCall call = new RescueCall();
        call.id = UUID.randomUUID();
        call.urgency = urgency;
        call.status = RescueStatus.PENDING;
        call.latitude = latitude;
        call.longitude = longitude;
        call.address = address;
        call.description = description;
        call.symptoms = new LinkedHashSet<>(symptoms);
        call.clientRequestId = clientRequestId;
        call.requestedByUserId = requestedByUserId;
        call.createdAt = now;
        call.updatedAt = now;
        call.eventSequence = 1;
        return call;
    }

    public void beginMatching(Instant now) {
        requireStatus(RescueStatus.PENDING);
        status = RescueStatus.MATCHING;
        matchDeadlineAt = now.plus(MATCH_WINDOW);
        touch(now);
    }

    public void assignDevice(
        EmergencyDevice device,
        Instant at,
        int distanceMeters,
        int arrivalSeconds,
        String strategy
    ) {
        requireStatus(RescueStatus.MATCHING);
        matchedDevice = device;
        matchedAt = at;
        matchedDistanceMeters = distanceMeters;
        estimatedArrivalSeconds = arrivalSeconds;
        matchedSnapshotLatitude = device.getLatitude();
        matchedSnapshotLongitude = device.getLongitude();
        matchedSnapshotAddress = device.getAddress();
        matchStrategy = strategy;
        aedCustodyStatus = AedCustodyStatus.RESERVED;
        touch(at);
    }

    public void acceptBy(UUID responderId, Instant now) {
        requireStatus(RescueStatus.MATCHING);
        if (responderUserId != null || matchedDevice == null) {
            throw invalid("该任务已有人响应或尚未匹配 AED");
        }
        responderUserId = responderId;
        acceptedAt = now;
        if (matchedDevice.getType() == DeviceType.FIXED) {
            status = RescueStatus.EN_ROUTE_TO_AED;
            aedCustodyStatus = AedCustodyStatus.PICKUP_PENDING;
        } else {
            status = RescueStatus.EN_ROUTE_TO_REQUESTER;
            aedCustodyStatus = AedCustodyStatus.IN_CUSTODY;
        }
        touch(now);
    }

    public void arriveAtAed(Instant now) {
        requireStatus(RescueStatus.EN_ROUTE_TO_AED);
        if (arrivedAtAedAt != null) {
            throw invalid("已经到达 AED 取用点");
        }
        arrivedAtAedAt = now;
        touch(now);
    }

    public void pickUpAed(Instant now) {
        requireStatus(RescueStatus.EN_ROUTE_TO_AED);
        if (arrivedAtAedAt == null) {
            throw invalid("请先确认到达 AED 取用点");
        }
        status = RescueStatus.EN_ROUTE_TO_REQUESTER;
        aedCustodyStatus = AedCustodyStatus.IN_CUSTODY;
        touch(now);
    }

    public void arriveAtRequester(Instant now) {
        requireStatus(RescueStatus.EN_ROUTE_TO_REQUESTER);
        status = RescueStatus.ARRIVED;
        arrivedAt = now;
        aedCustodyStatus = AedCustodyStatus.AT_SCENE;
        touch(now);
    }

    public void startRescue(Instant now) {
        requireStatus(RescueStatus.ARRIVED);
        status = RescueStatus.RESCUING;
        rescueStartedAt = now;
        touch(now);
    }

    public void submitCompletion(Instant now) {
        requireStatus(RescueStatus.RESCUING);
        status = RescueStatus.PENDING_CONFIRMATION;
        completionSubmittedAt = now;
        confirmationDeadlineAt = now.plus(CONFIRMATION_WINDOW);
        touch(now);
    }

    public void confirmCompletion(Instant now) {
        requireStatus(RescueStatus.PENDING_CONFIRMATION);
        status = RescueStatus.COMPLETED;
        completedAt = now;
        aedCustodyStatus = matchedDevice != null && matchedDevice.getType() == DeviceType.FIXED
            ? AedCustodyStatus.RETURNING
            : AedCustodyStatus.RETURNED;
        if (matchedDevice != null && matchedDevice.getType() == DeviceType.MOBILE) {
            matchedDevice.releaseReservation(id);
            aedReturnedAt = now;
        }
        touch(now);
    }

    public void returnAed(Instant now) {
        requireStatus(RescueStatus.COMPLETED);
        if (matchedDevice == null || matchedDevice.getType() != DeviceType.FIXED
            || aedCustodyStatus != AedCustodyStatus.RETURNING) {
            throw invalid("当前任务没有待归还的固定 AED");
        }
        aedCustodyStatus = AedCustodyStatus.RETURNED;
        aedReturnedAt = now;
        matchedDevice.releaseReservation(id);
        touch(now);
    }

    public void updateResponderLocation(UUID responderId, double latitude, double longitude, Instant now) {
        requireResponder(responderId);
        if (!status.isActive()) {
            throw invalid("任务已结束，不能继续上报位置");
        }
        responderLatitude = latitude;
        responderLongitude = longitude;
        responderLocationAt = now;
        touch(now);
    }

    public void addAttachment(UUID mediaId, UUID requesterId, Instant now) {
        if (!requestedByUserId.equals(requesterId)) {
            throw invalid("只有求救者可以上传现场附件");
        }
        if (status.isTerminal()) {
            throw invalid("任务已结束，不能继续上传附件");
        }
        if (attachmentMediaIds.size() >= 9) {
            throw invalid("每条求救最多上传 9 张图片");
        }
        if (!attachmentMediaIds.contains(mediaId)) {
            attachmentMediaIds.add(mediaId);
            touch(now);
        }
    }

    public void cancelByRequester(UUID requesterId, Instant now) {
        if (!requestedByUserId.equals(requesterId)) {
            throw invalid("只有求救者可以取消任务");
        }
        if (status != RescueStatus.PENDING && status != RescueStatus.MATCHING) {
            throw invalid("已有施救者响应，请联系施救者或管理员");
        }
        finishAs(RescueStatus.USER_CANCELLED, now);
    }

    public void finishAs(RescueStatus terminalStatus, Instant now) {
        if (!terminalStatus.isTerminal() || terminalStatus == RescueStatus.COMPLETED) {
            throw invalid("无效的救援终态");
        }
        if (status.isTerminal()) {
            throw invalid("救援任务已结束");
        }
        status = terminalStatus;
        if (matchedDevice != null) {
            matchedDevice.releaseReservation(id);
        }
        touch(now);
    }

    public void requireResponder(UUID userId) {
        if (responderUserId == null || !responderUserId.equals(userId)) {
            throw invalid("只能操作自己接取的救援任务");
        }
    }

    private void requireStatus(RescueStatus expected) {
        if (status != expected) {
            throw invalid("救援状态不能从 " + status + " 执行该操作");
        }
    }

    private void touch(Instant now) {
        updatedAt = now;
        eventSequence++;
    }

    private InvalidStateTransitionException invalid(String message) {
        return new InvalidStateTransitionException(message);
    }

    public UUID getId() { return id; }
    public UrgencyLevel getUrgency() { return urgency; }
    public RescueStatus getStatus() { return status; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public String getDescription() { return description; }
    public Set<String> getSymptoms() { return Set.copyOf(symptoms); }
    public List<UUID> getAttachmentMediaIds() { return List.copyOf(attachmentMediaIds); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getMatchDeadlineAt() { return matchDeadlineAt; }
    public EmergencyDevice getMatchedDevice() { return matchedDevice; }
    public Instant getMatchedAt() { return matchedAt; }
    public Integer getMatchedDistanceMeters() { return matchedDistanceMeters; }
    public Integer getEstimatedArrivalSeconds() { return estimatedArrivalSeconds; }
    public Double getMatchedSnapshotLatitude() { return matchedSnapshotLatitude; }
    public Double getMatchedSnapshotLongitude() { return matchedSnapshotLongitude; }
    public String getMatchedSnapshotAddress() { return matchedSnapshotAddress; }
    public String getMatchStrategy() { return matchStrategy; }
    public AedCustodyStatus getAedCustodyStatus() { return aedCustodyStatus; }
    public String getClientRequestId() { return clientRequestId; }
    public UUID getRequestedByUserId() { return requestedByUserId; }
    public UUID getResponderUserId() { return responderUserId; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getArrivedAtAedAt() { return arrivedAtAedAt; }
    public Instant getArrivedAt() { return arrivedAt; }
    public Instant getRescueStartedAt() { return rescueStartedAt; }
    public Instant getCompletionSubmittedAt() { return completionSubmittedAt; }
    public Instant getConfirmationDeadlineAt() { return confirmationDeadlineAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getAedReturnedAt() { return aedReturnedAt; }
    public Double getResponderLatitude() { return responderLatitude; }
    public Double getResponderLongitude() { return responderLongitude; }
    public Instant getResponderLocationAt() { return responderLocationAt; }
    public long getEventSequence() { return eventSequence; }
    public long getVersion() { return version; }
}
