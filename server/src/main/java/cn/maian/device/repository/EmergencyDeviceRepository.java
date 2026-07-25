package cn.maian.device.repository;

import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmergencyDeviceRepository extends JpaRepository<EmergencyDevice, UUID> {

    Page<EmergencyDevice> findAllByType(DeviceType type, Pageable pageable);
}
