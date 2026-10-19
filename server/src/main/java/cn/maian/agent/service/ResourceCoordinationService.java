package cn.maian.agent.service;

import cn.maian.agent.domain.CoordinationTask;
import cn.maian.agent.dto.CoordinationTaskResponse;
import cn.maian.agent.repository.CoordinationTaskRepository;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.config.DispatchProperties;
import cn.maian.device.repository.EmergencyDeviceRepository;
import cn.maian.device.domain.DeviceStatus;
import cn.maian.rescue.domain.*;
import cn.maian.rescue.repository.RescueCallRepository;
import cn.maian.rescue.repository.RescueEventRepository;
import cn.maian.rescue.service.AedDispatchService;
import cn.maian.rescue.service.RescueRealtimeHub;
import cn.maian.user.service.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class ResourceCoordinationService {
    private final CoordinationTaskRepository tasks;
    private final EmergencyDeviceRepository devices;
    private final RescueCallRepository calls;
    private final RescueEventRepository events;
    private final AedDispatchService dispatch;
    private final RescueRealtimeHub realtime;
    private final CurrentUserService users;
    private final DispatchProperties properties;
    private final Clock clock;

    public ResourceCoordinationService(CoordinationTaskRepository tasks, EmergencyDeviceRepository devices,
        RescueCallRepository calls, RescueEventRepository events, AedDispatchService dispatch,
        RescueRealtimeHub realtime, CurrentUserService users, DispatchProperties properties, Clock clock) {
        this.tasks = tasks; this.devices = devices; this.calls = calls; this.events = events;
        this.dispatch = dispatch; this.realtime = realtime; this.users = users;
        this.properties = properties; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<UUID> pending() { return tasks.findExecutable(clock.instant(), PageRequest.of(0, 10)); }

    @Transactional(readOnly = true)
    public List<UUID> awaitingFollowUp() { return tasks.findAttention(PageRequest.of(0, 30)); }

    @Transactional
    public boolean claim(UUID id) { return task(id).claim(clock.instant()); }

    @Transactional
    public String inspectDevice(UUID id) {
        var task = task(id);
        var device = devices.findById(task.getDeviceId()).orElse(null);
        String detail = device == null ? "设备已删除，已退出资源查询"
            : "设备状态：" + switch (device.getStatus()) {
                case AVAILABLE -> "可用";
                case RESERVED -> "救援占用";
                case DISABLED -> "停用维护";
                case PENDING_REVIEW -> "待审核";
                case REJECTED -> "审核未通过";
                case EXPIRED -> "已过期";
            } + "；当前"
                + (dispatch.isAvailableForDispatch(device, clock.instant()) ? "满足" : "不满足") + "调度条件";
        task.addStep("设备管理", detail, clock.instant());
        // Only controlled status values leave the server; no addresses, phone numbers or health data.
        return device == null ? "device_deleted" : "status=" + device.getStatus()
            + "; dispatchable=" + dispatch.isAvailableForDispatch(device, clock.instant());
    }

    @Transactional
    public String inspectImpact(UUID id) {
        var task = task(id);
        var call = task.getRescueCallId() == null ? null : calls.findDetailedById(task.getRescueCallId()).orElse(null);
        boolean associated = call != null && call.getMatchedDevice() != null
            && call.getMatchedDevice().getId().equals(task.getDeviceId());
        String detail = call != null && call.getStatus() == RescueStatus.MATCHING && call.getMatchedDevice() == null
            ? "原关联救援仍在等待可用 AED，将继续核实匹配结果"
            : !associated ? "当前无仍占用该设备的关联救援；将检查可重新匹配的等待任务"
            : call.getResponderUserId() == null ? "关联任务尚无志愿者接单，可按规则检查替代资源"
            : "已有志愿者响应，设备异常需要人工核实取用与保管安排";
        task.addStep("救援任务", detail, clock.instant());
        return "associated=" + associated + "; accepted=" + (associated && call.getResponderUserId() != null);
    }

    @Transactional
    public String findAlternatives(UUID id) {
        var task = task(id);
        var call = task.getRescueCallId() == null ? null : calls.findDetailedById(task.getRescueCallId()).orElse(null);
        if (call == null || (call.getMatchedDevice() != null
            && !call.getMatchedDevice().getId().equals(task.getDeviceId()))) {
            task.addStep("资源调度", "无待替换的关联设备，将在执行时检查等待任务", clock.instant());
            return "no_associated_rescue";
        }
        var alternatives = dispatch.previewAlternatives(call, task.getDeviceId());
        task.addStep("资源调度", "检查开放时段、有效期、在线状态与估算到达时间；找到 "
            + alternatives.size() + " 个候选（最多展示 3 个，执行时重新校验）", clock.instant());
        return "candidate_count=" + alternatives.size() + "; estimated_arrival_seconds="
            + alternatives.stream().map(AedDispatchService.CandidatePreview::estimatedArrivalSeconds).toList();
    }

    @Transactional
    public String coordinate(UUID id) {
        var task = task(id);
        if (task.isActionsApplied()) return task.getSummary();
        var now = clock.instant();
        // Lock order matches the rescue workflow: rescue first, then candidate devices.
        var call = task.getRescueCallId() == null ? null
            : calls.findDetailedForUpdateById(task.getRescueCallId()).orElse(null);
        if (call != null && call.getMatchedDevice() != null
            && call.getMatchedDevice().getId().equals(task.getDeviceId())) {
            if (call.getStatus().isTerminal() && call.getAedCustodyStatus() != AedCustodyStatus.RETURNING) {
                return applied(task, "关联救援已结束，设备保持当前状态", false);
            }
            if (dispatch.isAvailableForDispatch(call.getMatchedDevice(), now)) {
                return applied(task, "设备当前已恢复可用，保留现有救援安排", false);
            }
            if (call.getStatus() == RescueStatus.MATCHING && call.getResponderUserId() == null
                && call.getMatchDeadlineAt() != null && call.getMatchDeadlineAt().isAfter(now)) {
                call.clearUnavailableDevice(now);
                append(call, RescueEventType.RESOURCE_RECHECK, "设备异常，重新检查替代资源");
                if (assign(call)) return applied(task, "已释放原设备关联并重新匹配可用 AED，救援进展已同步", false);
                return applied(task, "原设备已退出本次匹配，暂未找到替代资源；系统继续在原时限内重试", true);
            }
            if (call.getStatus().isActive() || call.getAedCustodyStatus() == AedCustodyStatus.RETURNING) {
                append(call, RescueEventType.RESOURCE_RECHECK, "关联设备异常，请核实取用与保管安排");
                return applied(task, "关联救援已有执行安排，请设备管理方与志愿者人工核实；已保留设备保管关系", true);
            }
            return applied(task, "关联救援已结束，设备保持当前状态", false);
        }
        if (call != null) {
            if (call.getStatus() == RescueStatus.MATCHING && call.getMatchedDevice() == null) {
                return applied(task, "实时调度已移除失效设备，暂未找到替代资源；系统继续在原时限内重试", true);
            }
            return applied(task, "已复核：关联救援已由实时工作流调整资源或解除设备关联，本次无需重复修改", false);
        }
        var device = devices.findById(task.getDeviceId()).orElse(null);
        if (device == null || !"AED".equals(device.getCategory())
            || !dispatch.isAvailableForDispatch(device, now)) {
            return applied(task, "已检查设备状态与救援关联，当前无可自动调整的任务", false);
        }
        double lat = properties.searchRadiusKm() / 111.32;
        double lon = lat / Math.max(0.05, Math.cos(Math.toRadians(device.getLatitude())));
        var waiting = calls.findWaitingNear(device.getLatitude() - lat, device.getLatitude() + lat,
            device.getLongitude() - lon, device.getLongitude() + lon, now, PageRequest.of(0, 20));
        int matched = 0;
        for (UUID callId : waiting) {
            var waitingCall = calls.findDetailedForUpdateById(callId).orElse(null);
            if (waitingCall != null && waitingCall.getStatus() == RescueStatus.MATCHING
                && waitingCall.getMatchedDevice() == null && waitingCall.getMatchDeadlineAt().isAfter(now)
                && assign(waitingCall)) matched++;
        }
        return applied(task, "已检查附近 " + waiting.size() + " 个等待任务，完成 " + matched + " 次资源匹配", false);
    }

    private boolean assign(RescueCall call) {
        var result = dispatch.matchFastest(call);
        if (result.isEmpty()) return false;
        var match = result.orElseThrow();
        call.assignDevice(match.device(), match.matchedAt(), match.score().distanceMeters(),
            match.score().estimatedArrivalSeconds(), match.score().strategy());
        append(call, RescueEventType.AED_ASSIGNED, "资源协同已匹配可用 AED");
        return true;
    }

    private String applied(CoordinationTask task, String summary, boolean attention) {
        task.applied(summary, attention, clock.instant());
        task.addStep("协同执行", summary, clock.instant());
        return summary;
    }

    private void append(RescueCall call, RescueEventType type, String summary) {
        // RESOURCE_RECHECK without a state transition still needs its own event sequence.
        if (type == RescueEventType.RESOURCE_RECHECK) call.recordResourceRecheck(clock.instant());
        events.save(new RescueEvent(call.getId(), call.getEventSequence(), type, null, summary, clock.instant()));
        realtime.publishAfterCommit(call, type);
    }

    @Transactional
    public void complete(UUID id, String mode) {
        var task = task(id);
        task.complete(mode, clock.instant());
        if ("ENABLED".equals(task.getTriggerType())) {
            tasks.findAttentionForDevice(task.getDeviceId()).forEach(this::followUp);
        }
    }

    /** Reconcile outcomes without invoking the model or changing rescue assignments. */
    @Transactional
    public void followUp(UUID id) {
        var task = task(id);
        if (!task.needsFollowUp()) return;
        var now = clock.instant();
        // Rotate unresolved tasks through bounded batches without appending duplicate trace steps.
        task.checked(now);
        var call = task.getRescueCallId() == null ? null
            : calls.findDetailedById(task.getRescueCallId()).orElse(null);
        if (call == null) return;
        var matched = call.getMatchedDevice();
        if (matched != null && !matched.getId().equals(task.getDeviceId())) {
            if (call.getStatus() == RescueStatus.MATCHING && !dispatch.isAvailableForDispatch(matched, now)) return;
            task.resolve("后续调度已为关联救援匹配替代 AED，原设备保持当前状态", "救援任务", now);
            return;
        }
        if (call.getStatus().isTerminal() && call.getResponderUserId() == null) {
            task.resolve("关联救援已结束，无需继续等待替代资源；原设备保持当前状态", "救援任务", now);
            return;
        }
        if (matched == null) return;
        boolean restored = matched.getStatus() == DeviceStatus.AVAILABLE || matched.getStatus() == DeviceStatus.RESERVED;
        if (call.getId().equals(matched.getReservedForCallId()) && restored) {
            task.resolveRepair(now);
        } else if (call.getStatus().isTerminal() && call.getAedCustodyStatus() == AedCustodyStatus.RETURNED
            && restored) {
            task.resolve("关联救援的设备已归还，管理方已确认修复并恢复使用", "人工核实", now);
        }
    }

    @Transactional
    public void fail(UUID id) { task(id).fail(clock.instant()); }

    @Transactional
    public void retry(UUID id) {
        var task = task(id);
        if (!task.getOwnerUserId().equals(users.currentUserId())) throw new ResourceNotFoundException("协同任务不存在");
        task.retry(clock.instant());
    }

    @Transactional(readOnly = true)
    public List<CoordinationTaskResponse> mine() {
        return tasks.findByOwnerUserIdOrderByCreatedAtDesc(users.currentUserId(), PageRequest.of(0, 30))
            .stream().map(CoordinationTaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CoordinationTaskResponse> forRescue(UUID id) {
        var call = calls.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("救援任务不存在"));
        UUID userId = users.currentUserId();
        if (!userId.equals(call.getRequestedByUserId()) && !userId.equals(call.getResponderUserId())) {
            throw new ResourceNotFoundException("救援任务不存在");
        }
        return tasks.findByRescueCallIdOrderByCreatedAtDesc(id, PageRequest.of(0, 10))
            .stream().map(CoordinationTaskResponse::from).toList();
    }

    private CoordinationTask task(UUID id) {
        return tasks.findForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("协同任务不存在"));
    }
}
