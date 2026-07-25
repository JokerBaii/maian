package cn.maian.health.repository;

import cn.maian.health.domain.HealthReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthReportRepository extends JpaRepository<HealthReport, UUID> {
    List<HealthReport> findAllByUserIdOrderByCheckupDateDesc(UUID userId);
    Optional<HealthReport> findByIdAndUserId(UUID id, UUID userId);
}
