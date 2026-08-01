package cn.maian.rescue.service;

import cn.maian.config.DispatchProperties;
import cn.maian.rescue.domain.RescueStatus;
import cn.maian.rescue.repository.RescueCallRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReservationRecoveryService {

    private static final int RECOVERY_BATCH_SIZE = 100;

    private final RescueCallRepository rescueCallRepository;
    private final DispatchProperties dispatchProperties;

    public ReservationRecoveryService(
        RescueCallRepository rescueCallRepository,
        DispatchProperties dispatchProperties
    ) {
        this.rescueCallRepository = rescueCallRepository;
        this.dispatchProperties = dispatchProperties;
    }

    @Scheduled(
        initialDelayString = "${app.dispatch.recovery-initial-delay-ms:60000}",
        fixedDelayString = "${app.dispatch.recovery-interval-ms:60000}"
    )
    @Transactional
    public void releaseAbandonedMatchingReservations() {
        Instant now = Instant.now();
        Instant staleBefore = now.minusSeconds(dispatchProperties.reservationTimeoutSeconds());
        var staleCalls = rescueCallRepository.findStaleMatchingCalls(
            staleBefore,
            PageRequest.of(0, RECOVERY_BATCH_SIZE)
        );
        for (var rescueCall : staleCalls) {
            if (rescueCall.getMatchedDevice() != null) {
                rescueCall.getMatchedDevice().releaseReservation(rescueCall.getId());
            }
            rescueCall.transitionTo(RescueStatus.CANCELLED);
        }
    }
}
