package cn.maian.agent.service;

import cn.maian.agent.domain.CoordinationTask;
import cn.maian.agent.repository.CoordinationTaskRepository;
import cn.maian.device.domain.EmergencyDevice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.UUID;

@Service
public class CoordinationQueue {
    private final CoordinationTaskRepository tasks;
    private final Clock clock;
    public CoordinationQueue(CoordinationTaskRepository tasks, Clock clock) {
        this.tasks = tasks;
        this.clock = clock;
    }
    @Transactional
    public void enqueue(EmergencyDevice device, String trigger) {
        enqueue(device, trigger, UUID.randomUUID().toString());
    }
    @Transactional
    public void enqueue(EmergencyDevice device, String trigger, String key) {
        if (device.getRegisteredByUserId() == null || tasks.existsByEventKey(key)) return;
        tasks.save(CoordinationTask.create(device, trigger, key, clock.instant()));
    }
}
