package cn.maian.security;

import cn.maian.config.BootstrapAdminProperties;
import cn.maian.user.domain.UserProfile;
import cn.maian.user.repository.UserProfileRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {
    private final BootstrapAdminProperties properties;
    private final UserCredentialRepository credentialRepository;
    private final UserProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public BootstrapAdminInitializer(
        BootstrapAdminProperties properties,
        UserCredentialRepository credentialRepository,
        UserProfileRepository profileRepository,
        PasswordEncoder passwordEncoder,
        Clock clock
    ) {
        this.properties = properties;
        this.credentialRepository = credentialRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (profileRepository.existsByRole("ADMIN") || !properties.requested()) {
            return;
        }
        if (!properties.configured()) {
            throw new IllegalStateException("首个管理员手机号和密码必须同时配置");
        }
        if (!properties.phone().matches("^1[3-9]\\d{9}$")
            || properties.password().length() < 12 || properties.password().length() > 72) {
            throw new IllegalStateException("首个管理员手机号无效或密码长度不在 12 至 72 位之间");
        }

        if (credentialRepository.findByPhone(properties.phone()).isPresent()) {
            throw new IllegalStateException("首个管理员手机号已经注册，请使用未注册号码");
        }

        UUID userId = UUID.randomUUID();
        String nickname = properties.nickname() == null || properties.nickname().isBlank()
            ? "平台管理员"
            : properties.nickname().trim();
        if (nickname.length() > 60) {
            throw new IllegalStateException("首个管理员昵称不能超过 60 个字符");
        }
        var profile = UserProfile.register(userId, nickname, properties.phone(), clock.instant());
        profile.changeRole("ADMIN");
        profileRepository.save(profile);
        credentialRepository.save(new UserCredential(
            userId,
            properties.phone(),
            passwordEncoder.encode(properties.password()),
            clock.instant()
        ));
    }
}
