package cn.maian.security;

import cn.maian.config.DemoProperties;
import cn.maian.security.dto.AuthTokenResponse;
import cn.maian.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoAuthService {

    private final DemoProperties properties;
    private final UserProfileRepository userProfileRepository;
    private final JwtService jwtService;

    public DemoAuthService(
        DemoProperties properties,
        UserProfileRepository userProfileRepository,
        JwtService jwtService
    ) {
        this.properties = properties;
        this.userProfileRepository = userProfileRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(UUID userId) {
        if (!properties.enabled() || !DemoAccounts.IDS.contains(userId)) {
            throw new IllegalArgumentException("演示登录未启用或账号无效");
        }
        var profile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("演示账号不存在"));
        var token = jwtService.issue(profile.getId(), profile.getRole());
        return new AuthTokenResponse(token.accessToken(), token.expiresAt());
    }
}
