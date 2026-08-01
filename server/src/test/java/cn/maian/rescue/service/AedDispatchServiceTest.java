package cn.maian.rescue.service;

import cn.maian.config.DispatchProperties;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;
import cn.maian.device.repository.EmergencyDeviceRepository;
import cn.maian.rescue.domain.RescueCall;
import cn.maian.rescue.domain.UrgencyLevel;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AedDispatchServiceTest {

    private final EmergencyDeviceRepository repository = mock(EmergencyDeviceRepository.class);
    private final DispatchProperties properties = new DispatchProperties(
        15, 80, 120, 35, 6.5, 1.25, 30, 20, 900
    );
    private final AedDispatchService service = new AedDispatchService(repository, properties);

    @Test
    void reservesCandidateWithShortestEtaInsteadOfShortestStraightLineDistance() {
        RescueCall call = rescueCall();
        EmergencyDevice closerFixed = device(DeviceType.FIXED, 30.2827, null);
        EmergencyDevice fartherMobile = device(DeviceType.MOBILE, 30.2890, 5);
        when(repository.findDispatchCandidates(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            any(LocalDate.class), any(Instant.class), any(Pageable.class)
        )).thenReturn(List.of(closerFixed, fartherMobile));
        when(repository.reserveIfAvailable(eq(fartherMobile.getId()), eq(call.getId()), any(Instant.class)))
            .thenReturn(1);

        var result = service.matchFastest(call);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().device().getId()).isEqualTo(fartherMobile.getId());
        verify(repository).reserveIfAvailable(
            eq(fartherMobile.getId()), eq(call.getId()), any(Instant.class)
        );
    }

    @Test
    void fallsBackToNextCandidateWhenFastestWasClaimedConcurrently() {
        RescueCall call = rescueCall();
        EmergencyDevice first = device(DeviceType.MOBILE, 30.2830, 5);
        EmergencyDevice second = device(DeviceType.MOBILE, 30.2840, 5);
        when(repository.findDispatchCandidates(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            any(LocalDate.class), any(Instant.class), any(Pageable.class)
        )).thenReturn(List.of(first, second));
        when(repository.reserveIfAvailable(eq(first.getId()), eq(call.getId()), any(Instant.class)))
            .thenReturn(0);
        when(repository.reserveIfAvailable(eq(second.getId()), eq(call.getId()), any(Instant.class)))
            .thenReturn(1);

        var result = service.matchFastest(call);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().device().getId()).isEqualTo(second.getId());
    }

    private static RescueCall rescueCall() {
        return RescueCall.create(
            UrgencyLevel.CRITICAL,
            30.2800,
            120.1500,
            "测试呼救点",
            "测试",
            Set.of("心脏骤停"),
            List.of(),
            null
        );
    }

    private static EmergencyDevice device(
        DeviceType type,
        double latitude,
        Integer serviceRange
    ) {
        return EmergencyDevice.create(
            type,
            "AED",
            type + " AED",
            "测试位置",
            120.1500,
            latitude,
            "13500000000",
            "全天",
            null,
            "测试资源方",
            type == DeviceType.MOBILE ? "测试车辆" : null,
            serviceRange,
            null,
            List.of(),
            List.of()
        );
    }
}
