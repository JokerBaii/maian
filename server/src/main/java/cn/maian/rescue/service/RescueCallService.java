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
import cn.maian.user.service.UserProfileService;

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

    public RescueCallService(
        RescueCallRepository rescueCallRepository,
        AedDispatchService aedDispatchService
    ) {
        this.rescueCallRepository = rescueCallRepository;
        this.aedDispatchService = aedDispatchService;
    }

    @Transactional
    public RescueCallResponse create(CreateRescueCallRequest request) {
        if (request.clientRequestId() != null && !request.clientRequestId().isBlank()) {
            var existing = rescueCallRepository.findByClientRequestIdAndRequestedByUserId(
                request.clientRequestId(),
                UserProfileService.CURRENT_USER_ID
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
        rescueCall.requestBy(UserProfileService.CURRENT_USER_ID);
        rescueCall.beginMatching();
        rescueCall = rescueCallRepository.saveAndFlush(rescueCall);
        matchAndAssign(rescueCall);
        return RescueCallResponse.from(rescueCall);
    }

    @Transactional
    public RescueCallResponse retryMatching(UUID id) {
        RescueCall rescueCall = rescueCallRepository.findOwnedForMatchingById(
                id,
                UserProfileService.CURRENT_USER_ID
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
            .findAllOwnedDetailed(UserProfileService.CURRENT_USER_ID, pageable)
            .map(RescueCallResponse::from);
    }

    @Transactional
    public RescueCallResponse updateStatus(UUID id, RescueStatus nextStatus) {
        RescueCall rescueCall = findEntity(id);
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
        return RescueCallResponse.from(rescueCall);
    }

    private RescueCall findEntity(UUID id) {
        return rescueCallRepository.findOwnedDetailedById(id, UserProfileService.CURRENT_USER_ID)
            .orElseThrow(() -> new ResourceNotFoundException("未找到救援请求：" + id));
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
