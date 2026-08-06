package cn.maian.health.dto;

import java.util.List;

public record HealthMonitoringResponse(
    int current,
    int min,
    int max,
    int avg,
    String status,
    String scene,
    List<HeartRatePoint> todayData,
    List<DailyHeartRate> weekData,
    List<DailyHeartRate> monthData,
    List<HeartRateAlert> alerts,
    WearableDevice wearable,
    int minHeartRate,
    int maxHeartRate
) {
    public record HeartRatePoint(String time, int value, String scene) {
    }

    public record DailyHeartRate(String date, int avg, int min, int max) {
    }

    public record HeartRateAlert(String time, int value, String type, String message) {
    }

    public record WearableDevice(
        String name,
        String type,
        boolean connected,
        int battery
    ) {
    }
}
