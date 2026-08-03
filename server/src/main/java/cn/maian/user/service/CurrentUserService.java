package cn.maian.user.service;

import cn.maian.common.exception.ForbiddenOperationException;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.user.context.DemoUserContext;
import cn.maian.user.domain.UserProfile;
import cn.maian.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CurrentUserService {

    private final DemoUserContext demoUserContext;
    private final UserProfileRepository userProfileRepository;

    public CurrentUserService(
        DemoUserContext demoUserContext,
        UserProfileRepository userProfileRepository
    ) {
        this.demoUserContext = demoUserContext;
        this.userProfileRepository = userProfileRepository;
    }

    public UUID currentUserId() {
        return demoUserContext.userId();
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
