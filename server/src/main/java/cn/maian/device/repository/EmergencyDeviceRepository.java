package cn.maian.device.repository;

import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.DeviceStatus;
import cn.maian.device.domain.EmergencyDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Collection;
import java.util.UUID;

public interface EmergencyDeviceRepository extends JpaRepository<EmergencyDevice, UUID> {

    Page<EmergencyDevice> findAllByType(DeviceType type, Pageable pageable);

    Page<EmergencyDevice> findAllByStatus(DeviceStatus status, Pageable pageable);
    Page<EmergencyDevice> findAllByStatusIn(Collection<DeviceStatus> statuses, Pageable pageable);
    Page<EmergencyDevice> findAllByTypeAndStatusIn(
        DeviceType type,
        Collection<DeviceStatus> statuses,
        Pageable pageable
    );
    Page<EmergencyDevice> findAllByStatusNotIn(Collection<DeviceStatus> statuses, Pageable pageable);
    Page<EmergencyDevice> findAllByTypeAndStatusNotIn(
        DeviceType type,
        Collection<DeviceStatus> statuses,
        Pageable pageable
    );
    Page<EmergencyDevice> findAllByRegisteredByUserId(UUID userId, Pageable pageable);
    Page<EmergencyDevice> findAllByRegisteredByUserIdAndType(
        UUID userId,
        DeviceType type,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select device from EmergencyDevice device
        where device.id = :id and device.registeredByUserId = :userId
        """)
    java.util.Optional<EmergencyDevice> findOwnedForUpdateById(
        @Param("id") UUID id,
        @Param("userId") UUID userId
    );

    @Query("""
        select device from EmergencyDevice device
        where device.id = :id and device.registeredByUserId = :userId
        """)
    java.util.Optional<EmergencyDevice> findOwnedById(
        @Param("id") UUID id,
        @Param("userId") UUID userId
    );

    @Query("""
        select device from EmergencyDevice device
        where device.category = 'AED'
          and device.type = :type
          and device.status = cn.maian.device.domain.DeviceStatus.AVAILABLE
          and device.latitude between :minLatitude and :maxLatitude
          and device.longitude between :minLongitude and :maxLongitude
          and (device.expireDate is null or device.expireDate >= :today)
          and (device.type = cn.maian.device.domain.DeviceType.FIXED
               or device.lastLocationAt >= :mobileFreshSince)
        order by ((device.latitude - :centerLatitude) * (device.latitude - :centerLatitude)
               + (device.longitude - :centerLongitude) * (device.longitude - :centerLongitude)) asc
        """)
    List<EmergencyDevice> findDispatchCandidates(
        @Param("type") DeviceType type,
        @Param("centerLatitude") double centerLatitude,
        @Param("centerLongitude") double centerLongitude,
        @Param("minLatitude") double minLatitude,
        @Param("maxLatitude") double maxLatitude,
        @Param("minLongitude") double minLongitude,
        @Param("maxLongitude") double maxLongitude,
        @Param("today") LocalDate today,
        @Param("mobileFreshSince") Instant mobileFreshSince,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select device from EmergencyDevice device where device.id = :deviceId")
    java.util.Optional<EmergencyDevice> findDispatchCandidateForUpdateById(
        @Param("deviceId") UUID deviceId
    );
}
