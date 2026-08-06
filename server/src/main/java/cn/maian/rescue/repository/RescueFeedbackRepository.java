package cn.maian.rescue.repository;

import cn.maian.rescue.domain.RescueFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RescueFeedbackRepository extends JpaRepository<RescueFeedback, UUID> {
    Optional<RescueFeedback> findByRescueCallId(UUID rescueCallId);
    boolean existsByRescueCallId(UUID rescueCallId);
}
