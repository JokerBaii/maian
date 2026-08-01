package cn.maian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dispatch")
public record DispatchProperties(
    double searchRadiusKm,
    int candidateLimit,
    long mobileLocationMaxAgeSeconds,
    double mobileSpeedKmh,
    double runnerSpeedKmh,
    double routeDistanceFactor,
    int mobileDispatchOverheadSeconds,
    int fixedPickupOverheadSeconds,
    long reservationTimeoutSeconds
) {
    public DispatchProperties {
        searchRadiusKm = searchRadiusKm > 0 ? Math.min(searchRadiusKm, 50) : 15;
        candidateLimit = candidateLimit > 0 ? Math.min(candidateLimit, 500) : 80;
        mobileLocationMaxAgeSeconds = mobileLocationMaxAgeSeconds > 0
            ? mobileLocationMaxAgeSeconds : 120;
        mobileSpeedKmh = mobileSpeedKmh > 0 ? mobileSpeedKmh : 35;
        runnerSpeedKmh = runnerSpeedKmh > 0 ? runnerSpeedKmh : 6.5;
        routeDistanceFactor = routeDistanceFactor >= 1 ? routeDistanceFactor : 1.25;
        mobileDispatchOverheadSeconds = mobileDispatchOverheadSeconds >= 0
            ? mobileDispatchOverheadSeconds : 30;
        fixedPickupOverheadSeconds = fixedPickupOverheadSeconds >= 0
            ? fixedPickupOverheadSeconds : 20;
        reservationTimeoutSeconds = reservationTimeoutSeconds >= 60
            ? reservationTimeoutSeconds : 900;
    }
}
