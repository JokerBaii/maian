package cn.maian.media.service;

import cn.maian.common.exception.ForbiddenOperationException;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.common.exception.StorageCapacityException;
import cn.maian.config.MediaProperties;
import cn.maian.media.domain.MediaAsset;
import cn.maian.media.domain.MediaPurpose;
import cn.maian.media.domain.MediaStatus;
import cn.maian.media.domain.MediaVisibility;
import cn.maian.media.repository.MediaAssetRepository;
import cn.maian.rescue.repository.RescueCallRepository;
import cn.maian.user.service.CurrentUserService;
import cn.maian.security.AuthorizationPolicy;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.Collection;
import java.util.HashSet;

@Service
public class MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(MediaStorageService.class);
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;

    private final MediaProperties properties;
    private final Path storageDirectory;
    private final MediaAssetRepository mediaAssetRepository;
    private final RescueCallRepository rescueCallRepository;
    private final CurrentUserService currentUserService;
    private final MediaTokenService mediaTokenService;
    private final Clock clock;
    private final AuthorizationPolicy authorizationPolicy;

    public MediaStorageService(
        MediaProperties properties,
        MediaAssetRepository mediaAssetRepository,
        RescueCallRepository rescueCallRepository,
        CurrentUserService currentUserService,
        MediaTokenService mediaTokenService,
        Clock clock,
        AuthorizationPolicy authorizationPolicy
    ) {
        this.properties = properties;
        this.storageDirectory = properties.directory().toAbsolutePath().normalize();
        this.mediaAssetRepository = mediaAssetRepository;
        this.rescueCallRepository = rescueCallRepository;
        this.currentUserService = currentUserService;
        this.mediaTokenService = mediaTokenService;
        this.clock = clock;
        this.authorizationPolicy = authorizationPolicy;
    }

    @PostConstruct
    void initializeStorage() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建媒体存储目录", exception);
        }
    }

    @Transactional
    public UploadResult store(MultipartFile file, MediaPurpose purpose) {
        UUID ownerId = currentUserService.currentUserId();
        if (file.isEmpty()) {
            throw new IllegalArgumentException("请选择需要上传的图片");
        }
        if (file.getSize() > properties.maxFileSize()) {
            throw new IllegalArgumentException("图片超过允许的大小上限");
        }
        checkCapacity(ownerId, file.getSize());

        NormalizedImage normalized = normalize(file);
        checkCapacity(ownerId, normalized.bytes().length);
        UUID mediaId = UUID.randomUUID();
        String storageKey = mediaId.toString().substring(0, 2) + "/" + mediaId + normalized.extension();
        Path target = resolveStorageKey(storageKey);

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, normalized.bytes(), StandardOpenOption.CREATE_NEW);
            MediaAsset asset = new MediaAsset(
                mediaId, ownerId, purpose, storageKey, normalized.contentType(),
                normalized.bytes().length, sha256(normalized.bytes()), clock.instant()
            );
            mediaAssetRepository.save(asset);
            return new UploadResult(asset, issueDownload(asset));
        } catch (RuntimeException | IOException exception) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("图片保存失败", exception);
        }
    }

    @Transactional(readOnly = true)
    public IssuedDownload issueDownload(UUID mediaId) {
        MediaAsset asset = findAsset(mediaId);
        authorizeRead(asset);
        return issueDownload(asset);
    }

    @Transactional(readOnly = true)
    public StoredContent loadSigned(UUID mediaId, String token) {
        mediaTokenService.validate(mediaId, token);
        MediaAsset asset = findAsset(mediaId);
        Path path = resolveStorageKey(asset.getStorageKey());
        if (!Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("媒体文件不存在");
        }
        try {
            return new StoredContent(
                asset,
                new UrlResource(path.toUri()),
                properties.useXAccel() ? properties.xAccelPrefix() + asset.getStorageKey() : null
            );
        } catch (IOException exception) {
            throw new IllegalStateException("媒体文件读取失败", exception);
        }
    }

    @Transactional(readOnly = true)
    public byte[] readOwned(UUID mediaId, MediaPurpose expectedPurpose) {
        MediaAsset asset = findAsset(mediaId);
        if (!authorizationPolicy.isOwner(asset.getOwnerUserId()) || asset.getPurpose() != expectedPurpose) {
            throw new ForbiddenOperationException("无权使用该媒体文件");
        }
        try {
            return Files.readAllBytes(resolveStorageKey(asset.getStorageKey()));
        } catch (IOException exception) {
            throw new IllegalStateException("媒体文件读取失败", exception);
        }
    }

    @Transactional
    public void attachOwned(
        UUID mediaId,
        MediaPurpose expectedPurpose,
        String referenceType,
        UUID referenceId,
        boolean makePublic
    ) {
        MediaAsset asset = findAsset(mediaId);
        if (!authorizationPolicy.isOwner(asset.getOwnerUserId()) || asset.getPurpose() != expectedPurpose) {
            throw new ForbiddenOperationException("无权绑定该媒体文件");
        }
        asset.attach(referenceType, referenceId, makePublic, clock.instant());
    }

    @Transactional
    public void syncOwnedReference(
        Collection<UUID> mediaIds,
        MediaPurpose expectedPurpose,
        String referenceType,
        UUID referenceId,
        boolean makePublic
    ) {
        var requestedIds = new HashSet<>(mediaIds);
        for (UUID mediaId : requestedIds) {
            MediaAsset asset = findAsset(mediaId);
            if (!authorizationPolicy.isOwner(asset.getOwnerUserId()) || asset.getPurpose() != expectedPurpose) {
                throw new ForbiddenOperationException("无权绑定该媒体文件");
            }
            asset.attach(referenceType, referenceId, makePublic, clock.instant());
        }
        mediaAssetRepository.findAllByReferenceTypeAndReferenceId(referenceType, referenceId).stream()
            .filter(asset -> asset.getPurpose() == expectedPurpose)
            .filter(asset -> !requestedIds.contains(asset.getId()))
            .forEach(asset -> asset.detach(referenceType, referenceId));
    }

    @Transactional
    public void setReferencePublic(String referenceType, UUID referenceId, boolean makePublic) {
        mediaAssetRepository.findAllByReferenceTypeAndReferenceId(referenceType, referenceId)
            .forEach(asset -> asset.setPublic(makePublic));
    }

    @Transactional
    public void detachReference(String referenceType, UUID referenceId) {
        mediaAssetRepository.findAllByReferenceTypeAndReferenceId(referenceType, referenceId)
            .forEach(asset -> asset.detach(referenceType, referenceId));
    }

    @Transactional
    public void deleteOwned(UUID mediaId) {
        MediaAsset asset = findAsset(mediaId);
        authorizationPolicy.requireOwner(asset.getOwnerUserId(), "无权删除该媒体文件");
        try {
            Files.deleteIfExists(resolveStorageKey(asset.getStorageKey()));
            mediaAssetRepository.delete(asset);
        } catch (IOException exception) {
            throw new IllegalStateException("媒体文件删除失败", exception);
        }
    }

    @Transactional
    public int garbageCollectOrphans() {
        Instant cutoff = clock.instant().minusSeconds(properties.orphanRetentionHours() * 3600);
        var assets = mediaAssetRepository.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            MediaStatus.UNATTACHED, cutoff
        );
        int deleted = 0;
        for (MediaAsset asset : assets) {
            try {
                Files.deleteIfExists(resolveStorageKey(asset.getStorageKey()));
                mediaAssetRepository.delete(asset);
                deleted++;
            } catch (IOException exception) {
                log.warn("Unable to delete orphan media {}", asset.getId(), exception);
            }
        }
        return deleted;
    }

    private void authorizeRead(MediaAsset asset) {
        if (asset.getVisibility() == MediaVisibility.PUBLIC) {
            return;
        }
        if (authorizationPolicy.isOwner(asset.getOwnerUserId()) || authorizationPolicy.isAdmin()) {
            return;
        }
        if ("RESCUE_CALL".equals(asset.getReferenceType()) && asset.getReferenceId() != null) {
            var call = rescueCallRepository.findById(asset.getReferenceId()).orElse(null);
            if (call != null && authorizationPolicy.isRescueParticipant(call)) {
                return;
            }
        }
        throw new ForbiddenOperationException("无权访问该媒体文件");
    }

    private IssuedDownload issueDownload(MediaAsset asset) {
        var token = mediaTokenService.issue(asset.getId());
        return new IssuedDownload(
            "/api/v1/media/" + asset.getId() + "/content?token=" + token.value(),
            token.expiresAt()
        );
    }

    private void checkCapacity(UUID ownerId, long incomingBytes) {
        long ownerBytes = mediaAssetRepository.totalBytesForOwner(ownerId);
        if (ownerBytes + incomingBytes > properties.perUserQuotaBytes()) {
            throw new StorageCapacityException("当前用户媒体存储配额已用完");
        }
        long totalBytes = mediaAssetRepository.totalBytesStored();
        double nextRatio = (totalBytes + incomingBytes) / (double) properties.capacityBytes();
        if (nextRatio >= properties.rejectionRatio()) {
            throw new StorageCapacityException("媒体存储空间不足，暂停上传");
        }
        if (nextRatio >= properties.warningRatio()) {
            log.warn("Media storage usage is above warning threshold: {}%", Math.round(nextRatio * 100));
        }
        try {
            if (Files.getFileStore(storageDirectory).getUsableSpace() < incomingBytes * 2) {
                throw new StorageCapacityException("服务器磁盘可用空间不足");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法检查媒体存储空间", exception);
        }
    }

    private NormalizedImage normalize(MultipartFile file) {
        try {
            byte[] source = file.getBytes();
            String contentType;
            String format;
            String extension;
            if (isJpeg(source)) {
                contentType = "image/jpeg";
                format = "jpg";
                extension = ".jpg";
            } else if (isPng(source)) {
                contentType = "image/png";
                format = "png";
                extension = ".png";
            } else {
                throw new IllegalArgumentException("仅支持内容有效的 JPG 或 PNG 图片");
            }
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(source));
            if (decoded == null || (long) decoded.getWidth() * decoded.getHeight() > MAX_IMAGE_PIXELS) {
                throw new IllegalArgumentException("图片无法解码或像素尺寸过大");
            }
            BufferedImage clean = "jpg".equals(format) ? toRgb(decoded) : decoded;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(clean, format, output)) {
                throw new IllegalArgumentException("图片格式不受支持");
            }
            return new NormalizedImage(output.toByteArray(), contentType, extension);
        } catch (IOException exception) {
            throw new IllegalStateException("图片处理失败", exception);
        }
    }

    private BufferedImage toRgb(BufferedImage source) {
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, target.getWidth(), target.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return target;
    }

    private boolean isJpeg(byte[] content) {
        return content.length >= 3 && (content[0] & 0xff) == 0xff
            && (content[1] & 0xff) == 0xd8 && (content[2] & 0xff) == 0xff;
    }

    private boolean isPng(byte[] content) {
        return content.length >= 8 && (content[0] & 0xff) == 0x89
            && content[1] == 0x50 && content[2] == 0x4e && content[3] == 0x47
            && content[4] == 0x0d && content[5] == 0x0a && content[6] == 0x1a && content[7] == 0x0a;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算媒体摘要", exception);
        }
    }

    private MediaAsset findAsset(UUID mediaId) {
        return mediaAssetRepository.findById(mediaId)
            .orElseThrow(() -> new ResourceNotFoundException("媒体文件不存在"));
    }

    private Path resolveStorageKey(String storageKey) {
        Path path = storageDirectory.resolve(storageKey).normalize();
        if (!path.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("媒体存储地址无效");
        }
        return path;
    }

    private record NormalizedImage(byte[] bytes, String contentType, String extension) {
    }

    public record IssuedDownload(String url, Instant expiresAt) {
    }

    public record UploadResult(MediaAsset asset, IssuedDownload download) {
    }

    public record StoredContent(MediaAsset asset, Resource resource, String xAccelPath) {
    }
}
