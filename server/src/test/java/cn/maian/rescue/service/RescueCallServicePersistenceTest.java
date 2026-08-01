package cn.maian.rescue.service;

import cn.maian.config.DispatchProperties;
import cn.maian.device.domain.DeviceStatus;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;
import cn.maian.device.repository.EmergencyDeviceRepository;
import cn.maian.rescue.domain.RescueStatus;
import cn.maian.rescue.domain.UrgencyLevel;
import cn.maian.rescue.dto.CreateRescueCallRequest;
import cn.maian.rescue.repository.RescueCallRepository;
import cn.maian.user.service.UserProfileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RescueCallServicePersistenceTest {

    @Autowired
    private EmergencyDeviceRepository deviceRepository;

    @Autowired
    private RescueCallRepository rescueCallRepository;

    @Autowired
    private EntityManager entityManager;

    private RescueCallService rescueCallService;

    @BeforeEach
    void setUp() {
        var properties = new DispatchProperties(15, 80, 120, 35, 6.5, 1.25, 30, 20, 900);
        rescueCallService = new RescueCallService(
            rescueCallRepository,
            new AedDispatchService(deviceRepository, properties)
        );
    }

    @Test
    void persistsMatchAndReleasesReservationWhenCallIsCancelled() {
        EmergencyDevice device = deviceRepository.save(EmergencyDevice.create(
            DeviceType.FIXED,
            "AED",
            "测试 AED",
            "测试位置",
            120.1505,
            30.2805,
            "13500000000",
            "全天",
            null,
            "测试资源方",
            null,
            null,
            null,
            List.of(),
            List.of()
        ));

        var created = rescueCallService.create(new CreateRescueCallRequest(
            UrgencyLevel.CRITICAL,
            30.2800,
            120.1500,
            "测试呼救点",
            "持久化回归测试",
            Set.of("心脏骤停"),
            List.of(),
            "persistence-test-request"
        ));

        entityManager.flush();
        entityManager.clear();

        var persistedCall = rescueCallRepository.findOwnedDetailedById(
            created.id(),
            UserProfileService.CURRENT_USER_ID
        ).orElseThrow();
        assertThat(persistedCall.getMatchedDevice()).isNotNull();
        assertThat(persistedCall.getMatchedDevice().getId()).isEqualTo(device.getId());
        assertThat(persistedCall.getEstimatedArrivalSeconds()).isPositive();

        rescueCallService.updateStatus(created.id(), RescueStatus.CANCELLED);
        entityManager.flush();
        entityManager.clear();

        EmergencyDevice released = deviceRepository.findById(device.getId()).orElseThrow();
        assertThat(released.getStatus()).isEqualTo(DeviceStatus.AVAILABLE);
        assertThat(released.getReservedForCallId()).isNull();
    }
}
