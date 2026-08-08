package cn.maian.security;

import cn.maian.common.exception.ForbiddenOperationException;
import cn.maian.rescue.domain.RescueCall;
import cn.maian.user.service.CurrentUserService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** 资源级授权统一位于 Service 边界，Controller 只负责 HTTP 映射。 */
@Component
public class AuthorizationPolicy {
    private final CurrentUserService currentUserService;

    public AuthorizationPolicy(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public UUID currentUserId() {
        return currentUserService.currentUserId();
    }

    public boolean isAdmin() {
        return "ADMIN".equals(currentUserService.currentProfile().getRole());
    }

    public boolean isOwner(UUID ownerUserId) {
        return currentUserId().equals(ownerUserId);
    }

    public boolean isRescueParticipant(RescueCall call) {
        UUID userId = currentUserId();
        return userId.equals(call.getRequestedByUserId()) || userId.equals(call.getResponderUserId());
    }

    public void requireOwner(UUID ownerUserId, String message) {
        if (!isOwner(ownerUserId)) throw new ForbiddenOperationException(message);
    }

    public void requireRequester(RescueCall call, String message) {
        if (!currentUserId().equals(call.getRequestedByUserId())) {
            throw new ForbiddenOperationException(message);
        }
    }

    public void requireRescueParticipantOrAdmin(RescueCall call, String message) {
        if (!isRescueParticipant(call) && !isAdmin()) {
            throw new ForbiddenOperationException(message);
        }
    }
}
