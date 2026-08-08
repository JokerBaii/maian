package cn.maian.rescue.repository;

import cn.maian.rescue.domain.RescueEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RescueEventRepository extends JpaRepository<RescueEvent, UUID> {
    List<RescueEvent> findByRescueCallIdAndSequenceGreaterThanOrderBySequenceAsc(UUID rescueCallId, long sequence);
}
