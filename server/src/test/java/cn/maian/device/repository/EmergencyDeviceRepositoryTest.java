package cn.maian.device.repository;

import cn.maian.device.domain.DeviceStatus;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class EmergencyDeviceRepositoryTest {

    @Autowired
    private EmergencyDeviceRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void narrowsCandidatesAndClaimsDeviceAtomically() {
        EmergencyDevice available = repository.save(device("AED", 30.2810, 120.1510));
        repository.save(device("急救包", 30.2805, 120.1505));
        repository.flush();

        var candidates = repository.findDispatchCandidates(
            30.2800,
            120.1500,
            30.20,
            30.35,
            120.05,
            120.25,
            LocalDate.now(),
            Instant.now().minusSeconds(120),
            PageRequest.of(0, 80)
        );

        assertThat(candidates).extracting(EmergencyDevice::getId)
            .containsExactly(available.getId());

        UUID rescueCallId = UUID.randomUUID();
        assertThat(repository.reserveIfAvailable(
            available.getId(), rescueCallId, Instant.now()
        )).isEqualTo(1);
        assertThat(repository.reserveIfAvailable(
            available.getId(), UUID.randomUUID(), Instant.now()
        )).isZero();

        repository.flush();
        entityManager.clear();
        assertThat(repository.findById(available.getId()).orElseThrow().getStatus())
            .isEqualTo(DeviceStatus.RESERVED);
    }

    private static EmergencyDevice device(
        String category,
        double latitude,
        double longitude
    ) {
        return EmergencyDevice.create(
            DeviceType.FIXED,
            category,
            category + " 设备",
            "测试位置",
            longitude,
            latitude,
            "13500000000",
            "全天",
            null,
            "测试资源方",
            null,
            null,
            null,
            List.of(),
            List.of()
        );
    }
}
