package cn.maian.security;

import cn.maian.common.exception.ForbiddenOperationException;
import cn.maian.security.dto.AuthTokenResponse;
import cn.maian.security.dto.LoginRequest;
import cn.maian.security.dto.RegisterRequest;
import cn.maian.user.domain.UserProfile;
import cn.maian.user.repository.UserProfileRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class PasswordAuthService {
    private final UserCredentialRepository credentialRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public PasswordAuthService(
        UserCredentialRepository credentialRepository,
        UserProfileRepository userProfileRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        Clock clock
    ) {
        this.credentialRepository = credentialRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(LoginRequest request) {
        UserCredential credential = credentialRepository.findByPhone(request.phone())
            .orElseThrow(() -> new ForbiddenOperationException("手机号或密码错误"));
        if (!credential.isEnabled() || !passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            throw new ForbiddenOperationException("手机号或密码错误");
        }
        UserProfile profile = userProfileRepository.findById(credential.getUserId())
            .orElseThrow(() -> new ForbiddenOperationException("账号不可用"));
        var token = jwtService.issue(profile.getId(), profile.getRole());
        return new AuthTokenResponse(token.accessToken(), token.expiresAt());
    }

    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        if (credentialRepository.findByPhone(request.phone()).isPresent()) {
            throw new IllegalArgumentException("该手机号已注册");
        }
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(UserProfile.register(
            userId, request.nickname().trim(), request.phone(), clock.instant()
        ));
        credentialRepository.save(new UserCredential(
            userId, request.phone(), passwordEncoder.encode(request.password()), clock.instant()
        ));
        var token = jwtService.issue(userId, "USER");
        return new AuthTokenResponse(token.accessToken(), token.expiresAt());
    }
}
