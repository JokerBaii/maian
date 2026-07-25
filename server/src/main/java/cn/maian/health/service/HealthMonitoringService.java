package cn.maian.health.service;

import cn.maian.health.dto.HealthMonitoringResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class HealthMonitoringService {

    private static final int[] HALF_HOUR_VALUES = {
        62, 60, 58, 56, 55, 54, 53, 55, 57, 58, 60, 63,
        65, 68, 72, 76, 78, 82, 85, 98, 112, 125, 95, 88,
        80, 78, 75, 73, 72, 70, 70, 69, 68, 75, 82, 95,
        90, 85, 76, 74, 74, 72, 72, 70, 68, 66, 65, 63
    };

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter ALERT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public HealthMonitoringResponse currentSummary() {
        var month = buildMonthData();
        return new HealthMonitoringResponse(
            72,
            48,
            142,
            73,
            "normal",
            "resting",
            buildTodayData(),
            month.subList(month.size() - 7, month.size()),
            month,
            buildAlerts(),
            new HealthMonitoringResponse.WearableDevice(
                "未绑定设备",
                "none",
                false,
                0
            )
        );
    }

    private List<HealthMonitoringResponse.HeartRatePoint> buildTodayData() {
        var points = new ArrayList<HealthMonitoringResponse.HeartRatePoint>(HALF_HOUR_VALUES.length);
        for (int index = 0; index < HALF_HOUR_VALUES.length; index++) {
            int hour = index / 2;
            int minute = index % 2 == 0 ? 0 : 30;
            String scene = sceneFor(hour, minute);
            points.add(new HealthMonitoringResponse.HeartRatePoint(
                "%02d:%02d".formatted(hour, minute),
                HALF_HOUR_VALUES[index],
                scene
            ));
        }
        return points;
    }

    private String sceneFor(int hour, int minute) {
        if (hour < 5 || hour >= 22) {
            return "sleeping";
        }
        double time = hour + minute / 60.0;
        if ((time >= 9 && time <= 11) || (time >= 17 && time <= 18.5)) {
            return "exercise";
        }
        return "resting";
    }

    private List<HealthMonitoringResponse.DailyHeartRate> buildMonthData() {
        LocalDate today = LocalDate.now();
        var result = new ArrayList<HealthMonitoringResponse.DailyHeartRate>(30);
        for (int offset = 29; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            int sequence = 29 - offset;
            int average = 71 + (sequence * 5 % 7);
            int minimum = 49 + (sequence * 3 % 9);
            int maximum = 104 + (sequence * 7 % 18);
            if (sequence == 7) maximum = 142;
            if (sequence == 20) maximum = 135;
            if (sequence == 25) minimum = 48;
            result.add(new HealthMonitoringResponse.DailyHeartRate(
                date.format(DAY_FORMAT),
                average,
                minimum,
                maximum
            ));
        }
        return result;
    }

    private List<HealthMonitoringResponse.HeartRateAlert> buildAlerts() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
            alert(now.minusDays(2).withHour(10).withMinute(15), 118, "high", "心率偏高，请注意休息"),
            alert(now.minusDays(4).withHour(6).withMinute(30), 48, "low", "心率偏低，如持续请就医"),
            alert(now.minusDays(8).withHour(9).withMinute(45), 135, "high", "运动中心率过高，请降低运动强度"),
            alert(now.minusDays(15).withHour(17).withMinute(20), 122, "high", "心率偏高，建议停止运动休息"),
            alert(now.minusDays(21).withHour(10).withMinute(30), 142, "high", "心率严重偏高，请立即停止活动并休息")
        );
    }

    private HealthMonitoringResponse.HeartRateAlert alert(
        LocalDateTime time,
        int value,
        String type,
        String message
    ) {
        return new HealthMonitoringResponse.HeartRateAlert(
            time.format(ALERT_FORMAT),
            value,
            type,
            message
        );
    }
}
