package cn.maian.user.service;

import cn.maian.common.exception.ForbiddenOperationException;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.user.dto.AdminUserResponse;
import cn.maian.user.repository.UserProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminUserService {
    private final UserProfileRepository profileRepository;
    private final CurrentUserService currentUserService;

    public AdminUserService(
        UserProfileRepository profileRepository,
        CurrentUserService currentUserService
    ) {
        this.profileRepository = profileRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> findAll(Pageable pageable) {
        currentUserService.requireAnyRole("ADMIN");
        return profileRepository.findAllByOrderByCreatedAtDesc(pageable).map(AdminUserResponse::from);
    }

    @Transactional
    public AdminUserResponse changeRole(UUID userId, String role) {
        currentUserService.requireAnyRole("ADMIN");
        UUID currentUserId = currentUserService.currentUserId();
        var profile = profileRepository.findForUpdateById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("账户不存在"));
        if (userId.equals(currentUserId) && !"ADMIN".equals(role)) {
            throw new ForbiddenOperationException("不能移除自己的管理员权限");
        }
        if ("ADMIN".equals(profile.getRole()) && !"ADMIN".equals(role)
            && profileRepository.countByRole("ADMIN") <= 1) {
            throw new ForbiddenOperationException("系统至少需要保留一名管理员");
        }
        profile.changeRole(role);
        return AdminUserResponse.from(profile);
    }
}
