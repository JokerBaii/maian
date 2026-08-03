package cn.maian.health.service;

import cn.maian.health.domain.HeartRateReading;
import cn.maian.health.repository.HeartRateReadingRepository;
import cn.maian.health.repository.WearableDeviceRepository;
import cn.maian.user.domain.UserSettings;
import cn.maian.user.context.DemoUserContext;
import cn.maian.user.service.CurrentUserService;
import cn.maian.user.service.UserSettingsService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthMonitoringServiceTest {

    private final HeartRateReadingRepository readingRepository = mock(HeartRateReadingRepository.class);
    private final WearableDeviceRepository wearableRepository = mock(WearableDeviceRepository.class);
    private final UserSettingsService settingsService = mock(UserSettingsService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final HealthMonitoringService service = new HealthMonitoringService(
        readingRepository,
        wearableRepository,
        settingsService,
        currentUserService
    );

    @Test
    void returnsHonestEmptyStateInsteadOfFabricatedMeasurements() {
        stubSettings();
        when(readingRepository.findAllByUserIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
            eq(DemoUserContext.DEFAULT_USER_ID), any(Instant.class)
        )).thenReturn(List.of());
        when(wearableRepository.findByUserId(DemoUserContext.DEFAULT_USER_ID))
            .thenReturn(Optional.empty());

        var response = service.currentSummary();

        assertThat(response.current()).isZero();
        assertThat(response.todayData()).isEmpty();
        assertThat(response.alerts()).isEmpty();
        assertThat(response.wearable().connected()).isFalse();
        assertThat(response.status()).isEqualTo("no_data");
    }

    @Test
    void derivesAlertsFromStoredReadingsAndUserThresholds() {
        stubSettings();
        Instant now = Instant.now();
        var readings = List.of(
            HeartRateReading.create(DemoUserContext.DEFAULT_USER_ID, null, 72, "resting", now.minusSeconds(60)),
            HeartRateReading.create(DemoUserContext.DEFAULT_USER_ID, null, 132, "exercise", now)
        );
        when(readingRepository.findAllByUserIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
            eq(DemoUserContext.DEFAULT_USER_ID), any(Instant.class)
        )).thenReturn(readings);
        when(wearableRepository.findByUserId(DemoUserContext.DEFAULT_USER_ID))
            .thenReturn(Optional.empty());

        var response = service.currentSummary();

        assertThat(response.current()).isEqualTo(132);
        assertThat(response.status()).isEqualTo("high");
        assertThat(response.alerts()).hasSize(1);
        assertThat(response.alerts().getFirst().value()).isEqualTo(132);
    }

    private void stubSettings() {
        when(currentUserService.currentUserId()).thenReturn(DemoUserContext.DEFAULT_USER_ID);
        when(settingsService.findOrCreate())
            .thenReturn(UserSettings.defaults(DemoUserContext.DEFAULT_USER_ID));
    }
}
