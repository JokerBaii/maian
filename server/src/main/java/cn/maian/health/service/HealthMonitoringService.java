package cn.maian.health.service;

import cn.maian.health.domain.HeartRateReading;
import cn.maian.health.dto.HealthMonitoringResponse;
import cn.maian.health.repository.HeartRateReadingRepository;
import cn.maian.health.repository.WearableDeviceRepository;
import cn.maian.user.service.CurrentUserService;
import cn.maian.user.service.UserSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.Duration;
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
    private static final int HYSTERESIS_BPM = 5;
    private static final int MIN_EPISODE_SAMPLES = 3;
    private static final Duration MIN_EPISODE_DURATION = Duration.ofSeconds(30);
    private static final Duration MAX_SAMPLE_GAP = Duration.ofMinutes(2);
    private static final Duration ALERT_COOLDOWN = Duration.ofMinutes(10);

    private final HeartRateReadingRepository heartRateReadingRepository;
    private final WearableDeviceRepository wearableDeviceRepository;
    private final UserSettingsService userSettingsService;
    private final CurrentUserService currentUserService;

    public HealthMonitoringService(
        HeartRateReadingRepository heartRateReadingRepository,
        WearableDeviceRepository wearableDeviceRepository,
        UserSettingsService userSettingsService,
        CurrentUserService currentUserService
    ) {
        this.heartRateReadingRepository = heartRateReadingRepository;
        this.wearableDeviceRepository = wearableDeviceRepository;
        this.userSettingsService = userSettingsService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public HealthMonitoringResponse currentSummary() {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(USER_ZONE);
        Instant monthStart = today.minusDays(29).atStartOfDay(USER_ZONE).toInstant();
        List<HeartRateReading> readings = heartRateReadingRepository
            .findAllByUserIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
                currentUserService.currentUserId(),
                monthStart
            );
        var settings = userSettingsService.findOrCreate();
        var wearable = wearableDeviceRepository.findByUserId(currentUserService.currentUserId());

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
                )),
            settings.getMinHeartRate(),
            settings.getMaxHeartRate()
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
        List<HeartRateEpisode> episodes = aggregateEpisodes(readings, minimum, maximum);
        Map<String, Instant> lastAlertByType = new java.util.HashMap<>();
        var alerts = new ArrayList<HealthMonitoringResponse.HeartRateAlert>();
        for (HeartRateEpisode episode : episodes) {
            Instant previous = lastAlertByType.get(episode.type());
            if (previous != null && Duration.between(previous, episode.startedAt()).compareTo(ALERT_COOLDOWN) < 0) {
                continue;
            }
            lastAlertByType.put(episode.type(), episode.startedAt());
            long minutes = Math.max(1, Duration.between(episode.startedAt(), episode.endedAt()).toMinutes());
            alerts.add(new HealthMonitoringResponse.HeartRateAlert(
                ALERT_FORMAT.format(episode.startedAt().atZone(USER_ZONE)),
                episode.extremeBpm(),
                episode.type(),
                "high".equals(episode.type())
                    ? "心率持续偏高约 " + minutes + " 分钟，请停止活动并休息"
                    : "心率持续偏低约 " + minutes + " 分钟，如伴有不适请及时就医"
            ));
        }
        return alerts.stream()
            .sorted(Comparator.comparing(HealthMonitoringResponse.HeartRateAlert::time).reversed())
            .limit(50)
            .toList();
    }

    private List<HeartRateEpisode> aggregateEpisodes(
        List<HeartRateReading> readings,
        int minimum,
        int maximum
    ) {
        var episodes = new ArrayList<HeartRateEpisode>();
        EpisodeBuilder active = null;
        for (HeartRateReading reading : readings) {
            String triggerType = reading.getBpm() > maximum
                ? "high"
                : reading.getBpm() < minimum ? "low" : null;
            if (active == null) {
                if (triggerType != null) active = new EpisodeBuilder(triggerType, reading);
                continue;
            }
            boolean gapTooLarge = Duration.between(active.lastAt, reading.getRecordedAt())
                .compareTo(MAX_SAMPLE_GAP) > 0;
            boolean recovered = "high".equals(active.type)
                ? reading.getBpm() <= maximum - HYSTERESIS_BPM
                : reading.getBpm() >= minimum + HYSTERESIS_BPM;
            if (gapTooLarge || recovered) {
                active.addIfQualified(episodes);
                active = triggerType == null ? null : new EpisodeBuilder(triggerType, reading);
                continue;
            }
            active.add(reading);
        }
        if (active != null) active.addIfQualified(episodes);
        return episodes;
    }

    private static final class EpisodeBuilder {
        private final String type;
        private final Instant startedAt;
        private Instant lastAt;
        private int samples;
        private int extremeBpm;

        private EpisodeBuilder(String type, HeartRateReading reading) {
            this.type = type;
            this.startedAt = reading.getRecordedAt();
            this.lastAt = reading.getRecordedAt();
            this.samples = 1;
            this.extremeBpm = reading.getBpm();
        }

        private void add(HeartRateReading reading) {
            lastAt = reading.getRecordedAt();
            samples++;
            extremeBpm = "high".equals(type)
                ? Math.max(extremeBpm, reading.getBpm())
                : Math.min(extremeBpm, reading.getBpm());
        }

        private void addIfQualified(List<HeartRateEpisode> episodes) {
            if (samples >= MIN_EPISODE_SAMPLES
                && Duration.between(startedAt, lastAt).compareTo(MIN_EPISODE_DURATION) >= 0) {
                episodes.add(new HeartRateEpisode(type, startedAt, lastAt, extremeBpm));
            }
        }
    }

    private record HeartRateEpisode(
        String type,
        Instant startedAt,
        Instant endedAt,
        int extremeBpm
    ) {
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
