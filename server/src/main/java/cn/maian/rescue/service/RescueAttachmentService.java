package cn.maian.rescue.service;

import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.media.domain.MediaPurpose;
import cn.maian.media.service.MediaStorageService;
import cn.maian.rescue.dto.RequesterRescueResponse;
import cn.maian.rescue.repository.RescueCallRepository;
import cn.maian.user.service.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class RescueAttachmentService {

    private final RescueCallRepository rescueCallRepository;
    private final MediaStorageService mediaStorageService;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public RescueAttachmentService(
        RescueCallRepository rescueCallRepository,
        MediaStorageService mediaStorageService,
        CurrentUserService currentUserService,
        Clock clock
    ) {
        this.rescueCallRepository = rescueCallRepository;
        this.mediaStorageService = mediaStorageService;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    @Transactional
    public RequesterRescueResponse attach(UUID rescueCallId, UUID mediaId) {
        UUID requesterId = currentUserService.currentUserId();
        var call = rescueCallRepository.findDetailedForUpdateById(rescueCallId)
            .orElseThrow(() -> new ResourceNotFoundException("未找到救援请求：" + rescueCallId));
        call.addAttachment(mediaId, requesterId, clock.instant());
        mediaStorageService.attachOwned(
            mediaId, MediaPurpose.RESCUE_ATTACHMENT, "RESCUE_CALL", rescueCallId, false
        );
        return RequesterRescueResponse.from(call);
    }
}
