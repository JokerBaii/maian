package cn.maian.rescue.repository;

import cn.maian.rescue.domain.RescueCall;
import cn.maian.rescue.domain.RescueStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RescueCallRepository extends JpaRepository<RescueCall, UUID> {

    @EntityGraph(attributePaths = "matchedDevice")
    @Query("select call from RescueCall call where call.id = :id and call.requestedByUserId = :userId")
    Optional<RescueCall> findOwnedDetailedById(UUID id, UUID userId);

    @EntityGraph(attributePaths = "matchedDevice")
    @Query("select call from RescueCall call where call.id = :id")
    Optional<RescueCall> findDetailedById(UUID id);

    @EntityGraph(attributePaths = "matchedDevice")
    @Query("select call from RescueCall call where call.requestedByUserId = :userId")
    Page<RescueCall> findAllOwnedDetailed(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "matchedDevice")
    @Query("""
        select call from RescueCall call
        where call.requestedByUserId = :userId and call.status in :statuses
        order by call.createdAt desc
        """)
    List<RescueCall> findActiveOwned(UUID userId, Collection<RescueStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = "matchedDevice")
    @Query("""
        select call from RescueCall call
        where call.responderUserId = :responderUserId
          and (
            call.status in :statuses
            or (
              call.status = cn.maian.rescue.domain.RescueStatus.COMPLETED
              and call.aedCustodyStatus = cn.maian.rescue.domain.AedCustodyStatus.RETURNING
            )
          )
        order by call.updatedAt desc
        """)
    List<RescueCall> findAssignedToResponder(
        UUID responderUserId,
        Collection<RescueStatus> statuses,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "matchedDevice")
    @Query("""
        select call from RescueCall call
        where call.responderUserId is null
          and call.status = cn.maian.rescue.domain.RescueStatus.MATCHING
          and call.matchedDevice is not null
          and call.matchDeadlineAt > :now
          and call.latitude between :minLatitude and :maxLatitude
          and call.longitude between :minLongitude and :maxLongitude
        order by call.createdAt asc
        """)
    List<RescueCall> findMatchingOfferCandidates(
        double minLatitude,
        double maxLatitude,
        double minLongitude,
        double maxLongitude,
        Instant now,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "matchedDevice")
    @Query("select call from RescueCall call where call.id = :id")
    Optional<RescueCall> findDetailedForUpdateById(UUID id);

    Optional<RescueCall> findByClientRequestIdAndRequestedByUserId(
        String clientRequestId,
        UUID requestedByUserId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "matchedDevice")
    @Query("""
        select call from RescueCall call
        where call.status = cn.maian.rescue.domain.RescueStatus.MATCHING
          and call.updatedAt < :retryBefore
        order by call.updatedAt asc
        """)
    List<RescueCall> findMatchingForScheduler(Instant retryBefore, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "matchedDevice")
    @Query("""
        select call from RescueCall call
        where call.status = cn.maian.rescue.domain.RescueStatus.PENDING_CONFIRMATION
          and call.confirmationDeadlineAt <= :now
        order by call.confirmationDeadlineAt asc
        """)
    List<RescueCall> findConfirmationTimeouts(Instant now, Pageable pageable);
}
