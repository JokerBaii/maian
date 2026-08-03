package cn.maian.rescue.service;

import cn.maian.common.exception.InvalidStateTransitionException;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.rescue.domain.RescueCall;
import cn.maian.rescue.domain.RescueStatus;
import cn.maian.rescue.dto.CreateRescueCallRequest;
import cn.maian.rescue.dto.RescueCallResponse;
import cn.maian.rescue.repository.RescueCallRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import cn.maian.user.service.CurrentUserService;

@Service
public class RescueCallService {

    private static final Map<RescueStatus, Set<RescueStatus>> ALLOWED_TRANSITIONS = Map.of(
        RescueStatus.PENDING, EnumSet.of(RescueStatus.MATCHING, RescueStatus.CANCELLED),
        RescueStatus.MATCHING, EnumSet.of(RescueStatus.ACCEPTED, RescueStatus.CANCELLED),
        RescueStatus.ACCEPTED, EnumSet.of(RescueStatus.RESCUING, RescueStatus.CANCELLED),
        RescueStatus.RESCUING, EnumSet.of(RescueStatus.COMPLETED, RescueStatus.CANCELLED),
        RescueStatus.COMPLETED, EnumSet.noneOf(RescueStatus.class),
        RescueStatus.CANCELLED, EnumSet.noneOf(RescueStatus.class)
    );

    private final RescueCallRepository rescueCallRepository;
    private final AedDispatchService aedDispatchService;
    private final CurrentUserService currentUserService;

    public RescueCallService(
        RescueCallRepository rescueCallRepository,
        AedDispatchService aedDispatchService,
        CurrentUserService currentUserService
    ) {
        this.rescueCallRepository = rescueCallRepository;
        this.aedDispatchService = aedDispatchService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public RescueCallResponse create(CreateRescueCallRequest request) {
        if (request.clientRequestId() != null && !request.clientRequestId().isBlank()) {
            var existing = rescueCallRepository.findByClientRequestIdAndRequestedByUserId(
                request.clientRequestId(),
                currentUserService.currentUserId()
            );
            if (existing.isPresent()) {
                return RescueCallResponse.from(existing.orElseThrow());
            }
        }
        RescueCall rescueCall = RescueCall.create(
            request.urgency(),
            request.latitude(),
            request.longitude(),
            request.address(),
            request.description(),
            request.symptoms(),
            request.imageUrls(),
            request.clientRequestId()
        );
        rescueCall.requestBy(currentUserService.currentUserId());
        rescueCall.beginMatching();
        rescueCall = rescueCallRepository.saveAndFlush(rescueCall);
        matchAndAssign(rescueCall);
        return RescueCallResponse.from(rescueCall);
    }

    @Transactional
    public RescueCallResponse retryMatching(UUID id) {
        RescueCall rescueCall = rescueCallRepository.findOwnedForMatchingById(
                id,
                currentUserService.currentUserId()
            )
            .orElseThrow(() -> new ResourceNotFoundException("未找到救援请求：" + id));
        if (rescueCall.getMatchedDevice() != null || rescueCall.getStatus() != RescueStatus.MATCHING) {
            return RescueCallResponse.from(rescueCall);
        }
        matchAndAssign(rescueCall);
        return RescueCallResponse.from(rescueCall);
    }

    @Transactional(readOnly = true)
    public RescueCallResponse findById(UUID id) {
        return RescueCallResponse.from(findEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<RescueCallResponse> findAll(Pageable pageable) {
        return rescueCallRepository
            .findAllOwnedDetailed(currentUserService.currentUserId(), pageable)
            .map(RescueCallResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<RescueCallResponse> findResponderTasks(Pageable pageable) {
        currentUserService.requireAnyRole("VOLUNTEER", "ADMIN");
        return rescueCallRepository
            .findResponderTasks(currentUserService.currentUserId(), pageable)
            .map(RescueCallResponse::from);
    }

    @Transactional
    public RescueCallResponse accept(UUID id) {
        currentUserService.requireAnyRole("VOLUNTEER", "ADMIN");
        var rescueCall = findForResponderUpdate(id);
        rescueCall.acceptBy(currentUserService.currentUserId());
        return RescueCallResponse.from(rescueCall);
    }

    @Transactional
    public RescueCallResponse updateResponderProgress(UUID id, RescueStatus nextStatus) {
        currentUserService.requireAnyRole("VOLUNTEER", "ADMIN");
        var rescueCall = findForResponderUpdate(id);
        if (!currentUserService.currentUserId().equals(rescueCall.getResponderUserId())) {
            throw new cn.maian.common.exception.ForbiddenOperationException("只能更新自己接取的救援任务");
        }
        if (nextStatus != RescueStatus.RESCUING && nextStatus != RescueStatus.COMPLETED) {
            throw new IllegalArgumentException("救援者只能更新为赶往现场或已完成");
        }
        transition(rescueCall, nextStatus);
        return RescueCallResponse.from(rescueCall);
    }

    @Transactional
    public RescueCallResponse updateStatus(UUID id, RescueStatus nextStatus) {
        RescueCall rescueCall = findEntity(id);
        if (nextStatus != RescueStatus.CANCELLED) {
            throw new cn.maian.common.exception.ForbiddenOperationException("呼救者只能取消救援请求");
        }
        transition(rescueCall, nextStatus);
        return RescueCallResponse.from(rescueCall);
    }

    private void transition(RescueCall rescueCall, RescueStatus nextStatus) {
        Set<RescueStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(
            rescueCall.getStatus(),
            Set.of()
        );
        if (!allowed.contains(nextStatus)) {
            throw new InvalidStateTransitionException(
                "救援状态不能从 " + rescueCall.getStatus() + " 变更为 " + nextStatus
            );
        }
        rescueCall.transitionTo(nextStatus);
        if ((nextStatus == RescueStatus.COMPLETED || nextStatus == RescueStatus.CANCELLED)
            && rescueCall.getMatchedDevice() != null) {
            rescueCall.getMatchedDevice().releaseReservation(rescueCall.getId());
        }
    }

    private RescueCall findEntity(UUID id) {
        return rescueCallRepository.findOwnedDetailedById(id, currentUserService.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("未找到救援请求：" + id));
    }

    private RescueCall findForResponderUpdate(UUID id) {
        return rescueCallRepository.findDetailedForUpdateById(id)
            .orElseThrow(() -> new ResourceNotFoundException("未找到救援任务：" + id));
    }

    private void matchAndAssign(RescueCall rescueCall) {
        aedDispatchService.matchFastest(rescueCall).ifPresent(match -> rescueCall.assignDevice(
            match.device(),
            match.matchedAt(),
            match.score().distanceMeters(),
            match.score().estimatedArrivalSeconds(),
            match.score().strategy()
        ));
    }
}
