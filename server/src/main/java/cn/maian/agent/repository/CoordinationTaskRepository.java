package cn.maian.agent.repository;

import cn.maian.agent.domain.CoordinationTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoordinationTaskRepository extends JpaRepository<CoordinationTask, UUID> {
    boolean existsByEventKey(String eventKey);
    List<CoordinationTask> findByOwnerUserIdOrderByCreatedAtDesc(UUID userId, Pageable page);
    List<CoordinationTask> findByRescueCallIdOrderByCreatedAtDesc(UUID callId, Pageable page);
    @Query("select task.id from CoordinationTask task where task.deviceId = :deviceId and task.status = 'ATTENTION'")
    List<UUID> findAttentionForDevice(UUID deviceId);
    @Query("select task.id from CoordinationTask task where task.status = 'ATTENTION' order by task.updatedAt, task.id")
    List<UUID> findAttention(Pageable page);
    @Query("select task.id from CoordinationTask task where task.status = 'PENDING' or (task.status = 'RUNNING' and task.leaseUntil <= :now) order by task.createdAt")
    List<UUID> findExecutable(Instant now, Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from CoordinationTask task where task.id = :id")
    Optional<CoordinationTask> findForUpdate(UUID id);
}
