package cn.maian.rescue.repository;

import cn.maian.rescue.domain.RescueCall;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

public interface RescueCallRepository extends JpaRepository<RescueCall, UUID> {

    @EntityGraph(attributePaths = "matchedDevice")
    @Query("""
        select rescueCall from RescueCall rescueCall
        where rescueCall.id = :id and rescueCall.requestedByUserId = :userId
        """)
    Optional<RescueCall> findOwnedDetailedById(UUID id, UUID userId);

    @EntityGraph(attributePaths = "matchedDevice")
    @Query("""
        select rescueCall from RescueCall rescueCall
        where rescueCall.requestedByUserId = :userId
        """)
    Page<RescueCall> findAllOwnedDetailed(UUID userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "matchedDevice")
    @Query("""
        select rescueCall from RescueCall rescueCall
        where rescueCall.id = :id and rescueCall.requestedByUserId = :userId
        """)
    Optional<RescueCall> findOwnedForMatchingById(UUID id, UUID userId);

    Optional<RescueCall> findByClientRequestIdAndRequestedByUserId(
        String clientRequestId,
        UUID requestedByUserId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "matchedDevice")
    @Query("""
        select rescueCall from RescueCall rescueCall
        where rescueCall.status = cn.maian.rescue.domain.RescueStatus.MATCHING
          and rescueCall.updatedAt < :staleBefore
        order by rescueCall.updatedAt asc
        """)
    List<RescueCall> findStaleMatchingCalls(Instant staleBefore, Pageable pageable);
}
