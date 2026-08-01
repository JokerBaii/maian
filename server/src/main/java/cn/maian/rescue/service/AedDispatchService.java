package cn.maian.rescue.service;

import cn.maian.config.DispatchProperties;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;
import cn.maian.device.repository.EmergencyDeviceRepository;
import cn.maian.rescue.domain.RescueCall;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Optional;

@Service
public class AedDispatchService {

    private static final double LATITUDE_KM_PER_DEGREE = 111.32;
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Shanghai");

    private final EmergencyDeviceRepository emergencyDeviceRepository;
    private final DispatchProperties properties;

    public AedDispatchService(
        EmergencyDeviceRepository emergencyDeviceRepository,
        DispatchProperties properties
    ) {
        this.emergencyDeviceRepository = emergencyDeviceRepository;
        this.properties = properties;
    }

    public Optional<DispatchResult> matchFastest(RescueCall rescueCall) {
        Instant now = Instant.now();
        double latitudeRadius = properties.searchRadiusKm() / LATITUDE_KM_PER_DEGREE;
        double longitudeScale = Math.max(
            0.05,
            Math.cos(Math.toRadians(rescueCall.getLatitude()))
        );
        double longitudeRadius = properties.searchRadiusKm()
            / (LATITUDE_KM_PER_DEGREE * longitudeScale);

        var rankedCandidates = emergencyDeviceRepository.findDispatchCandidates(
                rescueCall.getLatitude(),
                rescueCall.getLongitude(),
                rescueCall.getLatitude() - latitudeRadius,
                rescueCall.getLatitude() + latitudeRadius,
                rescueCall.getLongitude() - longitudeRadius,
                rescueCall.getLongitude() + longitudeRadius,
                LocalDate.ofInstant(now, SERVICE_ZONE),
                now.minusSeconds(properties.mobileLocationMaxAgeSeconds()),
                PageRequest.of(0, properties.candidateLimit())
            ).stream()
            .map(device -> toCandidate(rescueCall, device, now))
            .filter(candidate -> candidate.score().eligible())
            .filter(candidate -> candidate.score().distanceMeters()
                <= properties.searchRadiusKm() * 1_000)
            .sorted(Comparator
                .comparingInt((RankedCandidate candidate) -> candidate.score().estimatedArrivalSeconds())
                .thenComparing(candidate -> candidate.device().getType() == DeviceType.MOBILE ? 0 : 1)
                .thenComparingInt(candidate -> candidate.score().distanceMeters()))
            .toList();

        for (RankedCandidate candidate : rankedCandidates) {
            int reserved = emergencyDeviceRepository.reserveIfAvailable(
                candidate.device().getId(),
                rescueCall.getId(),
                now
            );
            if (reserved == 1) {
                return Optional.of(new DispatchResult(candidate.device(), candidate.score(), now));
            }
        }
        return Optional.empty();
    }

    private RankedCandidate toCandidate(RescueCall rescueCall, EmergencyDevice device, Instant now) {
        return new RankedCandidate(
            device,
            AedDispatchScorer.score(
                rescueCall.getLatitude(),
                rescueCall.getLongitude(),
                device,
                now,
                properties
            )
        );
    }

    private record RankedCandidate(
        EmergencyDevice device,
        AedDispatchScorer.DispatchScore score
    ) {
    }

    public record DispatchResult(
        EmergencyDevice device,
        AedDispatchScorer.DispatchScore score,
        Instant matchedAt
    ) {
    }
}
