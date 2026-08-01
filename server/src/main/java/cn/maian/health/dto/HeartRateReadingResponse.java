package cn.maian.health.dto;

import cn.maian.health.domain.HeartRateReading;

import java.time.Instant;

public record HeartRateReadingResponse(
    long id,
    int bpm,
    String scene,
    Instant recordedAt
) {
    public static HeartRateReadingResponse from(HeartRateReading reading) {
        return new HeartRateReadingResponse(
            reading.getId(), reading.getBpm(), reading.getScene(), reading.getRecordedAt()
        );
    }
}
