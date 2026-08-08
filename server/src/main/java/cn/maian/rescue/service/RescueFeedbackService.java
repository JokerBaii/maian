package cn.maian.rescue.service;

import cn.maian.common.exception.ForbiddenOperationException;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.rescue.domain.RescueCall;
import cn.maian.rescue.domain.RescueFeedback;
import cn.maian.rescue.domain.RescueStatus;
import cn.maian.rescue.dto.CreateRescueFeedbackRequest;
import cn.maian.rescue.dto.RescueFeedbackResponse;
import cn.maian.rescue.repository.RescueCallRepository;
import cn.maian.rescue.repository.RescueFeedbackRepository;
import cn.maian.user.service.CurrentUserService;
import cn.maian.security.AuthorizationPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RescueFeedbackService {

    private final RescueFeedbackRepository feedbackRepository;
    private final RescueCallRepository rescueCallRepository;
    private final CurrentUserService currentUserService;
    private final AuthorizationPolicy authorizationPolicy;

    public RescueFeedbackService(
        RescueFeedbackRepository feedbackRepository,
        RescueCallRepository rescueCallRepository,
        CurrentUserService currentUserService,
        AuthorizationPolicy authorizationPolicy
    ) {
        this.feedbackRepository = feedbackRepository;
        this.rescueCallRepository = rescueCallRepository;
        this.currentUserService = currentUserService;
        this.authorizationPolicy = authorizationPolicy;
    }

    @Transactional
    public RescueFeedbackResponse submit(UUID rescueCallId, CreateRescueFeedbackRequest request) {
        RescueCall rescueCall = rescueCallRepository.findById(rescueCallId)
            .orElseThrow(() -> new ResourceNotFoundException("呼救不存在"));

        if (rescueCall.getStatus() != RescueStatus.COMPLETED) {
            throw new cn.maian.common.exception.InvalidStateTransitionException("救援完成后才能评价");
        }
        authorizationPolicy.requireRequester(rescueCall, "只有呼救方可以评价");
        if (feedbackRepository.existsByRescueCallId(rescueCallId)) {
            throw new cn.maian.common.exception.InvalidStateTransitionException("该呼救已评价过");
        }

        RescueFeedback feedback = new RescueFeedback(
            UUID.randomUUID(),
            rescueCallId,
            currentUserService.currentUserId(),
            rescueCall.getResponderUserId(),
            request.rating(),
            request.comment()
        );
        return RescueFeedbackResponse.from(feedbackRepository.save(feedback));
    }

    @Transactional(readOnly = true)
    public RescueFeedbackResponse getForRescue(UUID rescueCallId) {
        RescueCall rescueCall = rescueCallRepository.findById(rescueCallId)
            .orElseThrow(() -> new ResourceNotFoundException("呼救不存在"));
        authorizationPolicy.requireRescueParticipantOrAdmin(
            rescueCall, "只有救援参与者或管理员可查看评价"
        );
        return feedbackRepository.findByRescueCallId(rescueCallId)
            .map(RescueFeedbackResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException("暂无评价"));
    }
}
