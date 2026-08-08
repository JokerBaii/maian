package cn.maian.rescue.service;

import cn.maian.config.DispatchProperties;
import cn.maian.device.domain.DeviceStatus;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;
import cn.maian.device.repository.EmergencyDeviceRepository;
import cn.maian.rescue.domain.UrgencyLevel;
import cn.maian.rescue.dto.CreateRescueCallRequest;
import cn.maian.rescue.repository.ActiveRescueLockRepository;
import cn.maian.rescue.repository.RescueCallRepository;
import cn.maian.rescue.repository.RescueEventRepository;
import cn.maian.rescue.repository.ResponderPresenceRepository;
import cn.maian.security.DemoAccounts;
import cn.maian.user.repository.UserProfileRepository;
import cn.maian.user.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RescueCallServicePersistenceTest {

    @Autowired EmergencyDeviceRepository deviceRepository;
    @Autowired RescueCallRepository rescueCallRepository;
    @Autowired UserProfileRepository userProfileRepository;
    @Autowired ActiveRescueLockRepository activeRescueLockRepository;
    @Autowired ResponderPresenceRepository responderPresenceRepository;
    @Autowired RescueEventRepository rescueEventRepository;
    @Autowired EntityManager entityManager;

    private RescueCallService rescueCallService;

    @BeforeEach
    void setUp() {
        entityManager.createNativeQuery("""
            insert into user_profiles
              (id, nickname, phone, role, verified, created_at)
            values
              (:id, '测试用户', '未绑定', 'USER', false, CURRENT_TIMESTAMP)
            """)
            .setParameter("id", DemoAccounts.USER_ID.toString())
            .executeUpdate();
        entityManager.flush();

        DispatchProperties properties = new DispatchProperties(
            15, 80, 120, 35, 6.5, 1.25, 5, 90, 30, 20
        );
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.currentUserId()).thenReturn(DemoAccounts.USER_ID);
        Clock clock = Clock.fixed(Instant.parse("2026-08-08T10:00:00Z"), ZoneOffset.UTC);
        rescueCallService = new RescueCallService(
            rescueCallRepository,
            new AedDispatchService(deviceRepository, properties),
            currentUserService,
            userProfileRepository,
            activeRescueLockRepository,
            responderPresenceRepository,
            rescueEventRepository,
            properties,
            clock,
            mock(RescueRealtimeHub.class)
        );
    }

    @Test
    void persistsMatchAndReleasesReservationWhenRequesterCancels() {
        EmergencyDevice device = EmergencyDevice.create(
            DeviceType.FIXED, "AED", "测试 AED", "测试位置",
            120.1505, 30.2805, "13500000000", cn.maian.device.domain.DeviceServiceWindow.alwaysOpen(), null,
            "测试资源方", null, null, null, List.of(), List.of()
        );
        device.review(true, "测试审核通过");
        device = deviceRepository.save(device);

        var created = rescueCallService.create(new CreateRescueCallRequest(
            UrgencyLevel.CRITICAL, 30.2800, 120.1500, "测试呼救点",
            "持久化回归测试", Set.of("心脏骤停"), "persistence-test-request"
        ));

        entityManager.flush();
        entityManager.clear();

        var persistedCall = rescueCallRepository.findOwnedDetailedById(
            created.id(), DemoAccounts.USER_ID
        ).orElseThrow();
        assertThat(persistedCall.getMatchedDevice().getId()).isEqualTo(device.getId());
        assertThat(activeRescueLockRepository.findById(DemoAccounts.USER_ID)).isPresent();

        rescueCallService.cancel(created.id());
        entityManager.flush();
        entityManager.clear();

        EmergencyDevice released = deviceRepository.findById(device.getId()).orElseThrow();
        assertThat(released.getStatus()).isEqualTo(DeviceStatus.AVAILABLE);
        assertThat(released.getReservedForCallId()).isNull();
        assertThat(activeRescueLockRepository.findById(DemoAccounts.USER_ID)).isEmpty();
    }
}
