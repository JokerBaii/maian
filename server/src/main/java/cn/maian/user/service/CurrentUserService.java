package cn.maian.user.service;

import cn.maian.common.exception.ForbiddenOperationException;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.user.domain.UserProfile;
import cn.maian.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CurrentUserService {

    private final UserProfileRepository userProfileRepository;

    public CurrentUserService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getName())) {
            throw new ForbiddenOperationException("请先登录");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ForbiddenOperationException("登录身份无效");
        }
    }

    @Transactional(readOnly = true)
    public UserProfile currentProfile() {
        return userProfileRepository.findById(currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("当前用户不存在"));
    }

    @Transactional(readOnly = true)
    public void requireAnyRole(String... roles) {
        Set<String> allowed = Arrays.stream(roles).collect(Collectors.toSet());
        if (!allowed.contains(currentProfile().getRole())) {
            throw new ForbiddenOperationException("当前用户无权执行此操作");
        }
    }
}
