package cn.maian.rescue.service;

import cn.maian.rescue.domain.RescueEvent;
import cn.maian.rescue.domain.RescueEventType;
import cn.maian.rescue.domain.RescueStatus;
import cn.maian.rescue.repository.ActiveRescueLockRepository;
import cn.maian.rescue.repository.RescueCallRepository;
import cn.maian.rescue.repository.RescueEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class RescueWorkflowScheduler {

    private static final int BATCH_SIZE = 100;
    private static final long REMATCH_INTERVAL_SECONDS = 5;

    private final RescueCallRepository rescueCallRepository;
    private final ActiveRescueLockRepository activeRescueLockRepository;
    private final RescueEventRepository rescueEventRepository;
    private final AedDispatchService aedDispatchService;
    private final Clock clock;
    private final RescueRealtimeHub rescueRealtimeHub;

    public RescueWorkflowScheduler(
        RescueCallRepository rescueCallRepository,
        ActiveRescueLockRepository activeRescueLockRepository,
        RescueEventRepository rescueEventRepository,
        AedDispatchService aedDispatchService,
        Clock clock,
        RescueRealtimeHub rescueRealtimeHub
    ) {
        this.rescueCallRepository = rescueCallRepository;
        this.activeRescueLockRepository = activeRescueLockRepository;
        this.rescueEventRepository = rescueEventRepository;
        this.aedDispatchService = aedDispatchService;
        this.clock = clock;
        this.rescueRealtimeHub = rescueRealtimeHub;
    }

    @Scheduled(initialDelayString = "${app.dispatch.scheduler-initial-delay-ms:5000}", fixedDelayString = "${app.dispatch.scheduler-interval-ms:5000}")
    @Transactional
    public void rematchAndExpire() {
        Instant now = clock.instant();
        var calls = rescueCallRepository.findMatchingForScheduler(
            now.minusSeconds(REMATCH_INTERVAL_SECONDS), PageRequest.of(0, BATCH_SIZE)
        );
        for (var call : calls) {
            if (call.getMatchDeadlineAt() != null && !call.getMatchDeadlineAt().isAfter(now)) {
                RescueStatus terminal = call.getMatchedDevice() == null
                    ? RescueStatus.NO_RESOURCE
                    : RescueStatus.EXPIRED;
                call.finishAs(terminal, now);
                activeRescueLockRepository.deleteByRescueCallId(call.getId());
                append(call, terminal == RescueStatus.NO_RESOURCE
                    ? RescueEventType.NO_RESOURCE : RescueEventType.EXPIRED, now);
                continue;
            }
            if (call.getMatchedDevice() == null) {
                aedDispatchService.matchFastest(call).ifPresent(match -> {
                    call.assignDevice(
                        match.device(), match.matchedAt(), match.score().distanceMeters(),
                        match.score().estimatedArrivalSeconds(), match.score().strategy()
                    );
                    append(call, RescueEventType.AED_ASSIGNED, match.matchedAt());
                });
            }
        }
    }

    @Scheduled(initialDelayString = "${app.dispatch.confirmation-initial-delay-ms:30000}", fixedDelayString = "${app.dispatch.confirmation-interval-ms:30000}")
    @Transactional
    public void autoConfirmCompletedRescues() {
        Instant now = clock.instant();
        var calls = rescueCallRepository.findConfirmationTimeouts(now, PageRequest.of(0, BATCH_SIZE));
        for (var call : calls) {
            call.confirmCompletion(now);
            activeRescueLockRepository.deleteByRescueCallId(call.getId());
            append(call, RescueEventType.COMPLETION_AUTO_CONFIRMED, now);
        }
    }

    private void append(cn.maian.rescue.domain.RescueCall call, RescueEventType type, Instant now) {
        rescueEventRepository.save(new RescueEvent(
            call.getId(), call.getEventSequence(), type, null, summary(type), now
        ));
        rescueRealtimeHub.publishAfterCommit(call, type);
    }

    private String summary(RescueEventType type) {
        return switch (type) {
            case AED_ASSIGNED -> "系统已重新匹配 AED";
            case NO_RESOURCE -> "匹配时限内未找到可用资源";
            case EXPIRED -> "救援任务等待接单超时";
            case COMPLETION_AUTO_CONFIRMED -> "求救者未在时限内操作，系统自动确认完成";
            default -> type.name();
        };
    }
}
