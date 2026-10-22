package cn.maian.agent.repository;

import cn.maian.agent.domain.AssistantReminder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.*;

public interface AssistantReminderRepository extends JpaRepository<AssistantReminder,UUID> {
    long countByOwnerIdAndStatusNot(UUID ownerId,String status);
    List<AssistantReminder> findByOwnerIdAndStatusNotOrderByCreatedAtDesc(UUID ownerId,String status,Pageable pageable);
    @Query("select r.id from AssistantReminder r where r.status='WAITING' and r.nextCheckAt<=:now order by r.nextCheckAt")
    List<UUID> pending(Instant now,Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from AssistantReminder r where r.id=:id")
    Optional<AssistantReminder> lock(UUID id);
}
