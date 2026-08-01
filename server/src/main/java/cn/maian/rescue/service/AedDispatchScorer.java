package cn.maian.rescue.service;

import cn.maian.config.DispatchProperties;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.regex.Pattern;

public final class AedDispatchScorer {

    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern SERVICE_WINDOW = Pattern.compile(
        "(\\d{1,2}):(\\d{2})\\s*[-–至]\\s*(\\d{1,2}):(\\d{2})"
    );

    private AedDispatchScorer() {
    }

    public static DispatchScore score(
        double incidentLatitude,
        double incidentLongitude,
        EmergencyDevice device,
        Instant now,
        DispatchProperties properties
    ) {
        if (!isServiceAvailable(device, now)) {
            return DispatchScore.ineligible(Double.POSITIVE_INFINITY);
        }

        double distanceMeters = haversineMeters(
            incidentLatitude,
            incidentLongitude,
            device.getLatitude(),
            device.getLongitude()
        );

        if (device.getType() == DeviceType.MOBILE
            && device.getServiceRange() != null
            && distanceMeters > device.getServiceRange() * 1_000.0) {
            return DispatchScore.ineligible(distanceMeters);
        }

        double routeMeters = distanceMeters * properties.routeDistanceFactor();
        double travelSeconds;
        String strategy;
        if (device.getType() == DeviceType.MOBILE) {
            double metersPerSecond = properties.mobileSpeedKmh() / 3.6;
            double freshnessPenalty = locationFreshnessPenalty(device.getLastLocationAt(), now);
            travelSeconds = properties.mobileDispatchOverheadSeconds()
                + routeMeters / metersPerSecond
                + freshnessPenalty;
            strategy = "ETA_V1_MOBILE";
        } else {
            double metersPerSecond = properties.runnerSpeedKmh() / 3.6;
            // A fixed AED must be reached and brought back to the patient.
            travelSeconds = properties.fixedPickupOverheadSeconds()
                + (routeMeters * 2) / metersPerSecond;
            strategy = "ETA_V1_FIXED_RETRIEVAL";
        }

        return new DispatchScore(
            true,
            Math.max(0, (int) Math.round(distanceMeters)),
            Math.max(1, (int) Math.ceil(travelSeconds)),
            strategy
        );
    }

    public static double haversineMeters(
        double latitudeA,
        double longitudeA,
        double latitudeB,
        double longitudeB
    ) {
        double latitudeDelta = Math.toRadians(latitudeB - latitudeA);
        double longitudeDelta = Math.toRadians(longitudeB - longitudeA);
        double sinLatitude = Math.sin(latitudeDelta / 2);
        double sinLongitude = Math.sin(longitudeDelta / 2);
        double a = sinLatitude * sinLatitude
            + Math.cos(Math.toRadians(latitudeA))
            * Math.cos(Math.toRadians(latitudeB))
            * sinLongitude * sinLongitude;
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double locationFreshnessPenalty(Instant lastLocationAt, Instant now) {
        if (lastLocationAt == null) {
            return 60;
        }
        long ageSeconds = Math.max(0, Duration.between(lastLocationAt, now).toSeconds());
        return Math.min(30, ageSeconds * 0.15);
    }

    static boolean isServiceAvailable(EmergencyDevice device, Instant now) {
        LocalDate currentDate = LocalDate.ofInstant(now, SERVICE_ZONE);
        if (device.getExpireDate() != null && device.getExpireDate().isBefore(currentDate)) {
            return false;
        }

        String serviceTime = device.getServiceTime();
        if (serviceTime == null || serviceTime.isBlank() || serviceTime.contains("全天")) {
            return true;
        }

        DayOfWeek day = currentDate.getDayOfWeek();
        if (serviceTime.contains("工作日")
            && (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)) {
            return false;
        }

        var matcher = SERVICE_WINDOW.matcher(serviceTime);
        if (!matcher.find()) {
            return true;
        }

        try {
            LocalTime start = LocalTime.of(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2))
            );
            LocalTime end = LocalTime.of(
                Integer.parseInt(matcher.group(3)),
                Integer.parseInt(matcher.group(4))
            );
            LocalTime current = LocalTime.ofInstant(now, SERVICE_ZONE);
            if (start.equals(end)) return true;
            if (start.isBefore(end)) {
                return !current.isBefore(start) && !current.isAfter(end);
            }
            return !current.isBefore(start) || !current.isAfter(end);
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    public record DispatchScore(
        boolean eligible,
        int distanceMeters,
        int estimatedArrivalSeconds,
        String strategy
    ) {
        private static DispatchScore ineligible(double distanceMeters) {
            return new DispatchScore(
                false,
                Double.isFinite(distanceMeters)
                    ? Math.max(0, (int) Math.round(distanceMeters))
                    : Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                "INELIGIBLE"
            );
        }
    }
}
