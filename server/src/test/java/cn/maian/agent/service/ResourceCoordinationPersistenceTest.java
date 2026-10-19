package cn.maian.agent.service;

import cn.maian.agent.repository.CoordinationTaskRepository;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.config.DispatchProperties;
import cn.maian.device.domain.*;
import cn.maian.device.repository.EmergencyDeviceRepository;
import cn.maian.rescue.domain.*;
import cn.maian.rescue.repository.*;
import cn.maian.rescue.service.*;
import cn.maian.user.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class ResourceCoordinationPersistenceTest {
    @Autowired CoordinationTaskRepository tasks;
    @Autowired EmergencyDeviceRepository devices;
    @Autowired RescueCallRepository calls;
    @Autowired RescueEventRepository events;
    @Autowired ActiveRescueLockRepository activeLocks;
    @Autowired EntityManager entityManager;
    private final UUID owner = UUID.randomUUID();
    private final UUID requester = UUID.randomUUID();
    private final UUID volunteer = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS), ZoneOffset.UTC);
    private CurrentUserService users;
    private CoordinationQueue queue;
    private ResourceCoordinationService service;
    private AedDispatchService dispatch;

    @BeforeEach
    void setUp() {
        users = mock(CurrentUserService.class);
        when(users.currentUserId()).thenReturn(owner);
        var properties = new DispatchProperties(3, 40, 120, 35, 18, 1.25, 5, 90, 30, 20);
        dispatch = new AedDispatchService(devices, properties);
        queue = new CoordinationQueue(tasks, clock);
        service = new ResourceCoordinationService(tasks, devices, calls, events, dispatch,
            mock(RescueRealtimeHub.class), users, properties, clock);
    }

    @Test
    void faultReplacesUnacceptedResourceAndPersistsTraceWithoutReenablingBrokenDevice() {
        var old = device("原设备", 30.2801);
        var replacement = device("替代设备", 30.2810);
        var call = call(old, false);
        var deadline = call.getMatchDeadlineAt();
        old.reportFault();
        queue.enqueue(old, "FAULT_REPORTED");
        var taskId = runNext();
        // A repeated tool call must not create another reservation or duplicate state transition.
        var sequence = call.getEventSequence();
        service.coordinate(taskId);
        assertThat(call.getEventSequence()).isEqualTo(sequence);
        entityManager.flush(); entityManager.clear();
        var persisted = calls.findDetailedById(call.getId()).orElseThrow();
        assertThat(persisted.getMatchedDevice().getId()).isEqualTo(replacement.getId());
        assertThat(persisted.getMatchDeadlineAt()).isEqualTo(deadline);
        assertThat(devices.findById(old.getId()).orElseThrow().getStatus()).isEqualTo(DeviceStatus.DISABLED);
        assertThat(devices.findById(old.getId()).orElseThrow().getReservedForCallId()).isNull();
        var result = tasks.findById(taskId).orElseThrow();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getSteps()).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void acceptedRescueRequiresHumanReviewAndRepairPreservesOriginalReservation() {
        var old = device("原设备", 30.2801);
        var replacement = device("替代设备", 30.2810);
        var call = call(old, true);
        old.reportFault();
        queue.enqueue(old, "FAULT_REPORTED");
        var taskId = runNext();
        assertThat(call.getMatchedDevice().getId()).isEqualTo(old.getId());
        assertThat(call.getStatus()).isEqualTo(RescueStatus.EN_ROUTE_TO_AED);
        assertThat(old.getReservedForCallId()).isEqualTo(call.getId());
        assertThat(replacement.getStatus()).isEqualTo(DeviceStatus.AVAILABLE);
        assertThat(tasks.findById(taskId).orElseThrow().getStatus()).isEqualTo("ATTENTION");
        call.arriveAtAed(clock.instant());
        assertThatThrownBy(() -> call.pickUpAed(clock.instant())).hasMessageContaining("故障");
        old.enable();
        assertThat(old.getStatus()).isEqualTo(DeviceStatus.RESERVED);
        queue.enqueue(old, "ENABLED");
        runNext();
        assertThat(tasks.findById(taskId).orElseThrow().getStatus()).isEqualTo("COMPLETED");
        call.pickUpAed(clock.instant());
        assertThat(call.getStatus()).isEqualTo(RescueStatus.EN_ROUTE_TO_REQUESTER);
    }

    @Test
    void noReplacementKeepsOriginalDeadlineAndWaitingWorkflow() {
        var old = device("原设备", 30.2801);
        var call = call(old, false);
        var deadline = call.getMatchDeadlineAt();
        old.reportFault(); queue.enqueue(old, "FAULT_REPORTED");
        var taskId = runNext();
        assertThat(call.getMatchedDevice()).isNull();
        assertThat(call.getStatus()).isEqualTo(RescueStatus.MATCHING);
        assertThat(call.getMatchDeadlineAt()).isEqualTo(deadline);
        assertThat(tasks.findById(taskId).orElseThrow().getStatus()).isEqualTo("ATTENTION");
    }

    @Test
    void newlyAvailableDeviceMatchesWaitingRescueAndOnlyParticipantsCanReadRescueTrace() {
        var device = device("新资源", 30.2801);
        var call = call(null, false);
        queue.enqueue(device, "APPROVED");
        runNext();
        assertThat(call.getMatchedDevice().getId()).isEqualTo(device.getId());
        when(users.currentUserId()).thenReturn(UUID.randomUUID());
        assertThatThrownBy(() -> service.forRescue(call.getId())).isInstanceOf(ResourceNotFoundException.class);
        when(users.currentUserId()).thenReturn(requester);
        assertThat(service.forRescue(call.getId())).isEmpty(); // A resource owner's task cannot expose unrelated rescues.
    }

    @Test
    void schedulerRematchesBrokenResourceWithoutWaitingForModel() {
        var old = device("原设备", 30.2801);
        var replacement = device("替代设备", 30.2810);
        var call = call(old, false);
        old.reportFault();
        var scheduler = new RescueWorkflowScheduler(calls, activeLocks, events, dispatch, clock,
            mock(RescueRealtimeHub.class), queue);
        scheduler.rematchAndExpire();
        assertThat(call.getMatchedDevice().getId()).isEqualTo(replacement.getId());
        assertThat(old.getStatus()).isEqualTo(DeviceStatus.DISABLED);
        assertThat(service.pending()).hasSize(1);
    }

    @Test
    void schedulerClearingFaultWithoutReplacementStillRequiresFollowUp() {
        var old = device("原设备", 30.2801);
        var call = call(old, false);
        var deadline = call.getMatchDeadlineAt();
        old.reportFault();
        scheduler().rematchAndExpire();
        var id = runNext();
        assertThat(call.getMatchedDevice()).isNull();
        assertThat(call.getMatchDeadlineAt()).isEqualTo(deadline);
        assertThat(tasks.findById(id).orElseThrow().getStatus()).isEqualTo("ATTENTION");
        assertThat(tasks.findById(id).orElseThrow().getSummary()).contains("暂未找到替代资源");
        // A later normal dispatch pass resolves the existing coordination record.
        var replacement = device("后续可用设备", 30.2810);
        scheduler(Clock.offset(clock, java.time.Duration.ofSeconds(6))).rematchAndExpire();
        followUp();
        entityManager.flush(); entityManager.clear();
        assertThat(calls.findDetailedById(call.getId()).orElseThrow().getMatchedDevice().getId())
            .isEqualTo(replacement.getId());
        var resolved = tasks.findById(id).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo("COMPLETED");
        assertThat(resolved.getSummary()).contains("替代 AED");
        int steps = resolved.getSteps().size();
        service.followUp(id);
        assertThat(resolved.getSteps()).hasSize(steps);
    }

    @Test
    void waitingFollowUpStaysOpenUntilResourceArrivesOrRescueEnds() {
        var old = device("原设备", 30.2801);
        var call = call(old, false);
        old.reportFault(); queue.enqueue(old, "FAULT_REPORTED");
        var id = runNext();
        int steps = tasks.findById(id).orElseThrow().getSteps().size();
        followUp(); followUp();
        assertThat(tasks.findById(id).orElseThrow().getStatus()).isEqualTo("ATTENTION");
        assertThat(tasks.findById(id).orElseThrow().getSteps()).hasSize(steps);
        call.cancelByRequester(requester, clock.instant());
        followUp();
        assertThat(tasks.findById(id).orElseThrow().getStatus()).isEqualTo("COMPLETED");
        assertThat(old.getStatus()).isEqualTo(DeviceStatus.DISABLED);
    }

    @Test
    void repairAfterReturnResolvesOriginalAttentionWithoutReservation() {
        var old = device("原设备", 30.2801);
        var call = call(old, true);
        call.arriveAtAed(clock.instant()); call.pickUpAed(clock.instant());
        old.reportFault(); queue.enqueue(old, "FAULT_REPORTED");
        var faultId = runNext();
        finishAndReturn(call);
        followUp();
        assertThat(tasks.findById(faultId).orElseThrow().getStatus()).isEqualTo("ATTENTION");
        old.enable(); queue.enqueue(old, "ENABLED");
        var repairId = runNext();
        assertThat(tasks.findById(repairId).orElseThrow().getRescueCallId()).isNull();
        entityManager.flush(); entityManager.clear();
        assertThat(tasks.findById(faultId).orElseThrow().getStatus()).isEqualTo("COMPLETED");
        assertThat(tasks.findById(faultId).orElseThrow().getSummary()).contains("已归还", "修复");
    }

    @Test
    void repairQueuedBeforeReturnCanCompleteAfterReservationIsReleased() {
        var old = device("原设备", 30.2801);
        var call = call(old, true);
        call.arriveAtAed(clock.instant()); call.pickUpAed(clock.instant());
        old.reportFault(); queue.enqueue(old, "FAULT_REPORTED");
        var faultId = runNext();
        old.enable(); queue.enqueue(old, "ENABLED");
        finishAndReturn(call);
        runNext();
        assertThat(tasks.findById(faultId).orElseThrow().getStatus()).isEqualTo("COMPLETED");
        assertThat(old.getReservedForCallId()).isNull();
    }

    @Test
    void brokenDeviceRemainsDisabledAfterPhysicalReturn() {
        var device = device("原设备", 30.2801);
        var call = call(device, true);
        call.arriveAtAed(clock.instant()); call.pickUpAed(clock.instant());
        device.reportFault();
        call.arriveAtRequester(clock.instant()); call.startRescue(clock.instant());
        call.submitCompletion(clock.instant()); call.confirmCompletion(clock.instant());
        call.returnAed(clock.instant());
        entityManager.flush(); entityManager.clear();
        var returned = devices.findById(device.getId()).orElseThrow();
        assertThat(returned.getStatus()).isEqualTo(DeviceStatus.DISABLED);
        assertThat(returned.getReservedForCallId()).isNull();
    }

    @Test
    void failedModelFallsBackToActualResourceTools() {
        var old = device("原设备", 30.2801);
        var replacement = device("替代设备", 30.2810);
        var call = call(old, false);
        old.reportFault(); queue.enqueue(old, "FAULT_REPORTED");
        var gateway = mock(AgentModelGateway.class);
        when(gateway.invoke(anyString(), any(AgentModelGateway.BoundedTools.class))).thenReturn("RULE_FALLBACK");
        new ResourceCoordinationAgent(service, gateway).process();
        assertThat(call.getMatchedDevice().getId()).isEqualTo(replacement.getId());
        assertThat(service.mine()).singleElement().satisfies(task -> {
            assertThat(task.status()).isEqualTo("COMPLETED");
            assertThat(task.executionMode()).isEqualTo("RULE_FALLBACK");
        });
    }

    private UUID runNext() {
        entityManager.flush();
        UUID id = service.pending().getFirst();
        assertThat(service.claim(id)).isTrue();
        assertThat(service.claim(id)).isFalse();
        service.inspectDevice(id); service.inspectImpact(id); service.findAlternatives(id);
        service.coordinate(id); service.complete(id, "RULES");
        return id;
    }

    private RescueWorkflowScheduler scheduler() {
        return scheduler(clock);
    }

    private RescueWorkflowScheduler scheduler(Clock workflowClock) {
        return new RescueWorkflowScheduler(calls, activeLocks, events, dispatch, workflowClock,
            mock(RescueRealtimeHub.class), queue);
    }

    private void followUp() {
        entityManager.flush();
        new CoordinationFollowUpScheduler(service).process();
    }

    private void finishAndReturn(RescueCall call) {
        call.arriveAtRequester(clock.instant()); call.startRescue(clock.instant());
        call.submitCompletion(clock.instant()); call.confirmCompletion(clock.instant());
        call.returnAed(clock.instant());
    }

    private EmergencyDevice device(String name, double latitude) {
        var device = EmergencyDevice.create(DeviceType.FIXED, "AED", name, "测试地址", 120.15, latitude,
            null, DeviceServiceWindow.alwaysOpen(), null, null, null, null, null, List.of(), List.of());
        device.registerTo(owner); device.review(true, "审核通过");
        return devices.save(device);
    }

    private RescueCall call(EmergencyDevice device, boolean accepted) {
        var now = clock.instant().minusSeconds(20);
        var call = RescueCall.create(UrgencyLevel.CRITICAL, 30.28, 120.15, "测试现场", null,
            Set.of(), UUID.randomUUID().toString(), requester, now);
        call.beginMatching(now);
        if (device != null) {
            device.markReserved(call.getId(), now);
            call.assignDevice(device, now, 20, 60, "TEST");
            if (accepted) call.acceptBy(volunteer, now);
        }
        return calls.save(call);
    }
}
