package cn.maian.media.repository;

import cn.maian.media.domain.MediaAsset;
import cn.maian.media.domain.MediaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    @Query("select coalesce(sum(asset.sizeBytes), 0) from MediaAsset asset where asset.ownerUserId = :ownerUserId")
    long totalBytesForOwner(UUID ownerUserId);

    @Query("select coalesce(sum(asset.sizeBytes), 0) from MediaAsset asset")
    long totalBytesStored();

    List<MediaAsset> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
        MediaStatus status,
        Instant createdBefore
    );

    List<MediaAsset> findAllByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);
}
