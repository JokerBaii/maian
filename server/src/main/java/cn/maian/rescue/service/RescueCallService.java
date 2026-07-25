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

    public RescueCallService(RescueCallRepository rescueCallRepository) {
        this.rescueCallRepository = rescueCallRepository;
    }

    @Transactional
    public RescueCallResponse create(CreateRescueCallRequest request) {
        RescueCall rescueCall = RescueCall.create(
            request.urgency(),
            request.latitude(),
            request.longitude(),
            request.address(),
            request.description(),
            request.symptoms(),
            request.imageUrls()
        );
        return RescueCallResponse.from(rescueCallRepository.save(rescueCall));
    }

    @Transactional(readOnly = true)
    public RescueCallResponse findById(UUID id) {
        return RescueCallResponse.from(findEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<RescueCallResponse> findAll(Pageable pageable) {
        return rescueCallRepository.findAll(pageable).map(RescueCallResponse::from);
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
        return RescueCallResponse.from(rescueCall);
    }

    private RescueCall findEntity(UUID id) {
        return rescueCallRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("未找到救援请求：" + id));
    }
}
