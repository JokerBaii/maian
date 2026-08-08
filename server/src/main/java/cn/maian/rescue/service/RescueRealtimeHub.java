package cn.maian.rescue.service;

import cn.maian.rescue.domain.RescueCall;
import cn.maian.rescue.domain.RescueEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RescueRealtimeHub {
    private static final Logger log = LoggerFactory.getLogger(RescueRealtimeHub.class);
    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final Map<UUID, String> rolesByUser = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId, String role) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        rolesByUser.put(userId, role);
        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        send(userId, emitter, new RescueRealtimeEvent(null, "CONNECTED", 0, Instant.now()));
        return emitter;
    }

    public void publishAfterCommit(RescueCall call, RescueEventType type) {
        UUID requesterId = call.getRequestedByUserId();
        UUID responderId = call.getResponderUserId();
        RescueRealtimeEvent event = new RescueRealtimeEvent(
            call.getId(), type.name(), call.getEventSequence(), Instant.now()
        );
        Runnable publish = () -> {
            publishToUser(requesterId, event);
            if (responderId != null) publishToUser(responderId, event);
            if (type == RescueEventType.CREATED || type == RescueEventType.AED_ASSIGNED
                || type == RescueEventType.MATCHING_STARTED) {
                rolesByUser.forEach((userId, role) -> {
                    if ("VOLUNTEER".equals(role)) publishToUser(userId, event);
                });
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }

    private void publishToUser(UUID userId, RescueRealtimeEvent event) {
        List<SseEmitter> emitters = emittersByUser.getOrDefault(userId, new CopyOnWriteArrayList<>());
        emitters.forEach(emitter -> send(userId, emitter, event));
    }

    private void send(UUID userId, SseEmitter emitter, RescueRealtimeEvent event) {
        try {
            emitter.send(SseEmitter.event().name("rescue-update").data(event));
        } catch (IOException | IllegalStateException exception) {
            log.debug("Removing closed rescue event stream for user {}", userId);
            remove(userId, emitter);
        }
    }

    private void remove(UUID userId, SseEmitter emitter) {
        var emitters = emittersByUser.get(userId);
        if (emitters == null) return;
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId);
            rolesByUser.remove(userId);
        }
    }

    public record RescueRealtimeEvent(UUID rescueCallId, String type, long sequence, Instant occurredAt) {
    }
}
