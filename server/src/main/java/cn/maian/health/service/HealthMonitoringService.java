package cn.maian.health.service;

import cn.maian.health.domain.HeartRateReading;
import cn.maian.health.dto.HealthMonitoringResponse;
import cn.maian.health.repository.HeartRateReadingRepository;
import cn.maian.health.repository.WearableDeviceRepository;
import cn.maian.user.service.UserProfileService;
import cn.maian.user.service.UserSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HealthMonitoringService {

    private static final ZoneId USER_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ALERT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final HeartRateReadingRepository heartRateReadingRepository;
    private final WearableDeviceRepository wearableDeviceRepository;
    private final UserSettingsService userSettingsService;

    public HealthMonitoringService(
        HeartRateReadingRepository heartRateReadingRepository,
        WearableDeviceRepository wearableDeviceRepository,
        UserSettingsService userSettingsService
    ) {
        this.heartRateReadingRepository = heartRateReadingRepository;
        this.wearableDeviceRepository = wearableDeviceRepository;
        this.userSettingsService = userSettingsService;
    }

    @Transactional
    public HealthMonitoringResponse currentSummary() {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(USER_ZONE);
        Instant monthStart = today.minusDays(29).atStartOfDay(USER_ZONE).toInstant();
        List<HeartRateReading> readings = heartRateReadingRepository
            .findAllByUserIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
                UserProfileService.CURRENT_USER_ID,
                monthStart
            );
        var settings = userSettingsService.findOrCreate();
        var wearable = wearableDeviceRepository.findByUserId(UserProfileService.CURRENT_USER_ID);

        List<HeartRateReading> todayReadings = readings.stream()
            .filter(reading -> toDate(reading).equals(today))
            .toList();
        IntSummaryStatistics todayStats = todayReadings.stream()
            .mapToInt(HeartRateReading::getBpm)
            .summaryStatistics();
        HeartRateReading latest = readings.isEmpty() ? null : readings.get(readings.size() - 1);

        List<HealthMonitoringResponse.DailyHeartRate> month = buildDaily(readings);
        List<HealthMonitoringResponse.DailyHeartRate> week = month.stream()
            .filter(point -> !parseMonthDay(point.date(), today).isBefore(today.minusDays(6)))
            .toList();

        return new HealthMonitoringResponse(
            latest == null ? 0 : latest.getBpm(),
            todayStats.getCount() == 0 ? 0 : todayStats.getMin(),
            todayStats.getCount() == 0 ? 0 : todayStats.getMax(),
            todayStats.getCount() == 0 ? 0 : (int) Math.round(todayStats.getAverage()),
            statusFor(latest, settings.getMinHeartRate(), settings.getMaxHeartRate()),
            latest == null ? "resting" : latest.getScene(),
            todayReadings.stream().map(this::toPoint).toList(),
            week,
            month,
            settings.isHealthAlert()
                ? buildAlerts(readings, settings.getMinHeartRate(), settings.getMaxHeartRate())
                : List.of(),
            wearable
                .map(device -> new HealthMonitoringResponse.WearableDevice(
                    device.getName(),
                    device.getType(),
                    device.isConnected(),
                    device.getBattery() == null ? 0 : device.getBattery()
                ))
                .orElseGet(() -> new HealthMonitoringResponse.WearableDevice(
                    "未绑定设备", "none", false, 0
                ))
        );
    }

    private List<HealthMonitoringResponse.DailyHeartRate> buildDaily(List<HeartRateReading> readings) {
        Map<LocalDate, IntSummaryStatistics> byDay = new LinkedHashMap<>();
        readings.forEach(reading -> byDay
            .computeIfAbsent(toDate(reading), ignored -> new IntSummaryStatistics())
            .accept(reading.getBpm()));
        return byDay.entrySet().stream()
            .map(entry -> new HealthMonitoringResponse.DailyHeartRate(
                entry.getKey().format(DAY_FORMAT),
                (int) Math.round(entry.getValue().getAverage()),
                entry.getValue().getMin(),
                entry.getValue().getMax()
            ))
            .toList();
    }

    private List<HealthMonitoringResponse.HeartRateAlert> buildAlerts(
        List<HeartRateReading> readings,
        int minimum,
        int maximum
    ) {
        var alerts = new ArrayList<HealthMonitoringResponse.HeartRateAlert>();
        readings.stream()
            .filter(reading -> reading.getBpm() < minimum || reading.getBpm() > maximum)
            .sorted(Comparator.comparing(HeartRateReading::getRecordedAt).reversed())
            .limit(50)
            .forEach(reading -> {
                boolean high = reading.getBpm() > maximum;
                alerts.add(new HealthMonitoringResponse.HeartRateAlert(
                    ALERT_FORMAT.format(reading.getRecordedAt().atZone(USER_ZONE)),
                    reading.getBpm(),
                    high ? "high" : "low",
                    high ? "心率超过预警阈值，请停止活动并休息"
                        : "心率低于预警阈值，如持续不适请及时就医"
                ));
            });
        return alerts;
    }

    private HealthMonitoringResponse.HeartRatePoint toPoint(HeartRateReading reading) {
        return new HealthMonitoringResponse.HeartRatePoint(
            TIME_FORMAT.format(reading.getRecordedAt().atZone(USER_ZONE)),
            reading.getBpm(),
            reading.getScene()
        );
    }

    private String statusFor(HeartRateReading reading, int minimum, int maximum) {
        if (reading == null) return "no_data";
        if (reading.getBpm() < minimum) return "low";
        if (reading.getBpm() > maximum) return "high";
        return "normal";
    }

    private LocalDate toDate(HeartRateReading reading) {
        return reading.getRecordedAt().atZone(USER_ZONE).toLocalDate();
    }

    private LocalDate parseMonthDay(String monthDay, LocalDate today) {
        String[] parts = monthDay.split("-");
        LocalDate parsed = LocalDate.of(today.getYear(), Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        return parsed.isAfter(today) ? parsed.minusYears(1) : parsed;
    }
}
