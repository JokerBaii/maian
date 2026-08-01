package cn.maian.health.repository;

import cn.maian.health.domain.WearableDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WearableDeviceRepository extends JpaRepository<WearableDevice, UUID> {
    Optional<WearableDevice> findByUserId(UUID userId);
}
