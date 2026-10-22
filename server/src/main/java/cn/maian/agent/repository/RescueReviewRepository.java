package cn.maian.agent.repository;

import cn.maian.agent.domain.RescueReview;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.*;

public interface RescueReviewRepository extends JpaRepository<RescueReview, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select review from RescueReview review where review.rescueCallId = :id")
    Optional<RescueReview> findForUpdate(UUID id);
    @Query("select review.rescueCallId from RescueReview review where review.status = 'PENDING' or (review.status = 'RUNNING' and review.leaseUntil <= :now) order by review.updatedAt")
    List<UUID> findExecutable(Instant now, Pageable page);
}
