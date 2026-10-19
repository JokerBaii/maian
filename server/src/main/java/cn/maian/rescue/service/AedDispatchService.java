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
import java.util.List;
import java.util.stream.Stream;

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
        var rankedCandidates = rankedCandidates(rescueCall, now);
        for (RankedCandidate candidate : rankedCandidates) {
            var lockedDevice = emergencyDeviceRepository.findDispatchCandidateForUpdateById(
                candidate.device().getId()
            ).orElse(null);
            Instant lockedAt = Instant.now();
            if (lockedDevice != null && isAvailableForDispatch(lockedDevice, lockedAt)
                && lockedDevice.getStatus() == cn.maian.device.domain.DeviceStatus.AVAILABLE
                && lockedDevice.getReservedForCallId() == null) {
                var score = toCandidate(rescueCall, lockedDevice, lockedAt).score();
                if (!score.eligible() || score.distanceMeters() > properties.searchRadiusKm() * 1_000) continue;
                lockedDevice.markReserved(rescueCall.getId(), lockedAt);
                return Optional.of(new DispatchResult(lockedDevice, score, lockedAt));
            }
        }
        return Optional.empty();
    }

    public boolean isAvailableForDispatch(EmergencyDevice device, Instant now) {
        return "AED".equals(device.getCategory())
            && (device.getStatus() == cn.maian.device.domain.DeviceStatus.AVAILABLE
                || device.getStatus() == cn.maian.device.domain.DeviceStatus.RESERVED)
            && AedDispatchScorer.isServiceAvailable(device, now)
            && (device.getType() == DeviceType.FIXED
                || device.mobilePresenceAt(now, properties.mobileLocationMaxAgeSeconds())
                    == cn.maian.device.domain.MobilePresenceStatus.ONLINE);
    }

    public List<CandidatePreview> previewAlternatives(RescueCall call, java.util.UUID excludedId) {
        return rankedCandidates(call, Instant.now()).stream()
            .filter(candidate -> !candidate.device().getId().equals(excludedId))
            .limit(3)
            .map(candidate -> new CandidatePreview(candidate.device().getName(),
                candidate.score().estimatedArrivalSeconds(), candidate.score().strategy()))
            .toList();
    }

    private List<RankedCandidate> rankedCandidates(RescueCall rescueCall, Instant now) {
        double latitudeRadius = properties.searchRadiusKm() / LATITUDE_KM_PER_DEGREE;
        double longitudeScale = Math.max(
            0.05,
            Math.cos(Math.toRadians(rescueCall.getLatitude()))
        );
        double longitudeRadius = properties.searchRadiusKm()
            / (LATITUDE_KM_PER_DEGREE * longitudeScale);

        var fixedCandidates = emergencyDeviceRepository.findDispatchCandidates(
                DeviceType.FIXED,
                rescueCall.getLatitude(),
                rescueCall.getLongitude(),
                rescueCall.getLatitude() - latitudeRadius,
                rescueCall.getLatitude() + latitudeRadius,
                rescueCall.getLongitude() - longitudeRadius,
                rescueCall.getLongitude() + longitudeRadius,
                LocalDate.ofInstant(now, SERVICE_ZONE),
                now.minusSeconds(properties.mobileLocationMaxAgeSeconds()),
                PageRequest.of(0, properties.candidateLimitPerType())
            );
        var mobileCandidates = emergencyDeviceRepository.findDispatchCandidates(
                DeviceType.MOBILE,
                rescueCall.getLatitude(),
                rescueCall.getLongitude(),
                rescueCall.getLatitude() - latitudeRadius,
                rescueCall.getLatitude() + latitudeRadius,
                rescueCall.getLongitude() - longitudeRadius,
                rescueCall.getLongitude() + longitudeRadius,
                LocalDate.ofInstant(now, SERVICE_ZONE),
                now.minusSeconds(properties.mobileLocationMaxAgeSeconds()),
                PageRequest.of(0, properties.candidateLimitPerType())
            );

        return Stream.concat(fixedCandidates.stream(), mobileCandidates.stream())
            .map(device -> toCandidate(rescueCall, device, now))
            .filter(candidate -> candidate.score().eligible())
            .filter(candidate -> candidate.score().distanceMeters()
                <= properties.searchRadiusKm() * 1_000)
            .sorted(Comparator
                .comparingInt((RankedCandidate candidate) -> candidate.score().rankingScoreSeconds())
                .thenComparing(Comparator.comparingInt(
                    (RankedCandidate candidate) -> candidate.score().confidencePercent()
                ).reversed())
                .thenComparingInt(candidate -> candidate.score().estimatedArrivalSeconds())
                .thenComparing(candidate -> candidate.device().getType() == DeviceType.MOBILE ? 0 : 1)
                .thenComparingInt(candidate -> candidate.score().distanceMeters()))
            .toList();

    }

    public record CandidatePreview(String name, int estimatedArrivalSeconds, String strategy) {}

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
