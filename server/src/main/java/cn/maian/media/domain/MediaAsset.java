package cn.maian.media.domain;

import cn.maian.common.exception.InvalidStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
public class MediaAsset {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private MediaPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private MediaVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private MediaStatus status;

    @Column(length = 160, nullable = false, unique = true)
    private String storageKey;

    @Column(length = 40, nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(length = 64, nullable = false)
    private String sha256;

    @Column(length = 30)
    private String referenceType;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID referenceId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant attachedAt;

    protected MediaAsset() {
    }

    public MediaAsset(
        UUID id,
        UUID ownerUserId,
        MediaPurpose purpose,
        String storageKey,
        String contentType,
        long sizeBytes,
        String sha256,
        Instant createdAt
    ) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.purpose = purpose;
        this.visibility = MediaVisibility.PRIVATE;
        this.status = MediaStatus.UNATTACHED;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.createdAt = createdAt;
    }

    public void attach(String referenceType, UUID referenceId, boolean makePublic, Instant now) {
        if (status == MediaStatus.ATTACHED
            && (!this.referenceType.equals(referenceType) || !this.referenceId.equals(referenceId))) {
            throw new InvalidStateTransitionException("媒体文件已绑定其他业务记录");
        }
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.status = MediaStatus.ATTACHED;
        this.visibility = makePublic ? MediaVisibility.PUBLIC : MediaVisibility.PRIVATE;
        this.attachedAt = now;
    }

    public void setPublic(boolean makePublic) {
        if (status != MediaStatus.ATTACHED) {
            throw new InvalidStateTransitionException("未绑定的媒体不能发布");
        }
        this.visibility = makePublic ? MediaVisibility.PUBLIC : MediaVisibility.PRIVATE;
    }

    public void detach(String expectedReferenceType, UUID expectedReferenceId) {
        if (status != MediaStatus.ATTACHED
            || !expectedReferenceType.equals(referenceType)
            || !expectedReferenceId.equals(referenceId)) {
            return;
        }
        this.referenceType = null;
        this.referenceId = null;
        this.status = MediaStatus.UNATTACHED;
        this.visibility = MediaVisibility.PRIVATE;
        this.attachedAt = null;
    }

    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public MediaPurpose getPurpose() { return purpose; }
    public MediaVisibility getVisibility() { return visibility; }
    public MediaStatus getStatus() { return status; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAttachedAt() { return attachedAt; }
}
