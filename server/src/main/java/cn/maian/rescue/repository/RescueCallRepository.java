package cn.maian.rescue.repository;

import cn.maian.rescue.domain.RescueCall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RescueCallRepository extends JpaRepository<RescueCall, UUID> {
}
