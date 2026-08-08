package cn.maian.security;

import cn.maian.config.AuthProperties;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static java.time.temporal.ChronoUnit.SECONDS;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void issuedTokenCanBeDecodedWithConfiguredHs256Key() {
        AuthProperties properties = new AuthProperties(
            "maian-test", "0123456789abcdef0123456789abcdef", 15
        );
        SecurityConfiguration configuration = new SecurityConfiguration();
        JwtService jwtService = new JwtService(configuration.jwtEncoder(properties), properties);
        UUID userId = UUID.randomUUID();

        JwtService.IssuedToken issued = jwtService.issue(userId, "VOLUNTEER");
        var decoded = configuration.jwtDecoder(properties).decode(issued.accessToken());

        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getClaimAsString("iss")).isEqualTo("maian-test");
        assertThat(decoded.getClaimAsString("role")).isEqualTo("VOLUNTEER");
        assertThat(decoded.getExpiresAt()).isEqualTo(issued.expiresAt().truncatedTo(SECONDS));
    }
}
