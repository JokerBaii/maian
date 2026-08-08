package cn.maian.rescue.repository;

import cn.maian.rescue.domain.ActiveRescueLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ActiveRescueLockRepository extends JpaRepository<ActiveRescueLock, UUID> {
    Optional<ActiveRescueLock> findByRescueCallId(UUID rescueCallId);
    void deleteByRescueCallId(UUID rescueCallId);
}
