package cn.maian.rescue.service;

import cn.maian.config.DispatchProperties;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public final class AedDispatchScorer {

    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Shanghai");

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
        double uncertaintySeconds;
        String strategy;
        if (device.getType() == DeviceType.MOBILE) {
            double metersPerSecond = properties.mobileSpeedKmh() / 3.6;
            travelSeconds = properties.mobileDispatchOverheadSeconds()
                + routeMeters / metersPerSecond;
            uncertaintySeconds = mobileUncertaintySeconds(device, distanceMeters, now, properties);
            strategy = uncertaintySeconds <= 20 ? "ETA_V2_MOBILE_LIVE" : "ETA_V2_MOBILE_BUFFERED";
        } else {
            double metersPerSecond = properties.runnerSpeedKmh() / 3.6;
            // A fixed AED must be reached and brought back to the patient.
            travelSeconds = properties.fixedPickupOverheadSeconds()
                + (routeMeters * 2) / metersPerSecond;
            // Access control, lift and handover time make fixed-device retrieval less deterministic.
            uncertaintySeconds = 18;
            strategy = "ETA_V2_FIXED_RETRIEVAL";
        }

        int estimatedArrivalSeconds = Math.max(
            1,
            (int) Math.ceil(travelSeconds + uncertaintySeconds * 0.5)
        );
        int rankingScoreSeconds = Math.max(
            estimatedArrivalSeconds,
            (int) Math.ceil(travelSeconds + uncertaintySeconds)
        );
        int confidencePercent = Math.max(55, 100 - (int) Math.round(uncertaintySeconds));

        return new DispatchScore(
            true,
            Math.max(0, (int) Math.round(distanceMeters)),
            estimatedArrivalSeconds,
            rankingScoreSeconds,
            confidencePercent,
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
        a = Math.max(0, Math.min(1, a));
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double mobileUncertaintySeconds(
        EmergencyDevice device,
        double distanceMeters,
        Instant now,
        DispatchProperties properties
    ) {
        double locationAgeRatio = 1;
        if (device.getLastLocationAt() != null) {
            long ageSeconds = Math.max(0, Duration.between(device.getLastLocationAt(), now).toSeconds());
            locationAgeRatio = Math.min(
                1,
                ageSeconds / (double) properties.mobileLocationMaxAgeSeconds()
            );
        }
        double freshnessRisk = locationAgeRatio * 35;

        double edgeRisk = 0;
        if (device.getServiceRange() != null && device.getServiceRange() > 0) {
            double rangeUsage = distanceMeters / (device.getServiceRange() * 1_000.0);
            edgeRisk = Math.max(0, Math.min(1, (rangeUsage - 0.7) / 0.3)) * 25;
        }
        return freshnessRisk + edgeRisk;
    }

    static boolean isServiceAvailable(EmergencyDevice device, Instant now) {
        LocalDate currentDate = LocalDate.ofInstant(now, SERVICE_ZONE);
        if (device.getExpireDate() != null && device.getExpireDate().isBefore(currentDate)) {
            return false;
        }

        LocalTime current = LocalTime.ofInstant(now, SERVICE_ZONE);
        return device.isWithinServiceWindow(currentDate.getDayOfWeek(), current);
    }

    public record DispatchScore(
        boolean eligible,
        int distanceMeters,
        int estimatedArrivalSeconds,
        int rankingScoreSeconds,
        int confidencePercent,
        String strategy
    ) {
        private static DispatchScore ineligible(double distanceMeters) {
            return new DispatchScore(
                false,
                Double.isFinite(distanceMeters)
                    ? Math.max(0, (int) Math.round(distanceMeters))
                    : Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                0,
                "INELIGIBLE"
            );
        }
    }
}
