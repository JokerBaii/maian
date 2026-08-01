package cn.maian.health.repository;

import cn.maian.health.domain.HeartRateReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HeartRateReadingRepository extends JpaRepository<HeartRateReading, Long> {
    List<HeartRateReading> findAllByUserIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
        UUID userId,
        Instant recordedAfter
    );
}
