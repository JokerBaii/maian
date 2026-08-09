package cn.maian.security;

import cn.maian.config.AuthProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ProductionSecurityGuard {

    private static final String DEVELOPMENT_SECRET = "change-this-development-jwt-secret";

    private final Environment environment;
    private final AuthProperties authProperties;

    public ProductionSecurityGuard(
        Environment environment,
        AuthProperties authProperties
    ) {
        this.environment = environment;
        this.authProperties = authProperties;
    }

    @PostConstruct
    void validateProductionIsolation() {
        boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!production) {
            return;
        }
        if (DEVELOPMENT_SECRET.equals(authProperties.secret())) {
            throw new IllegalStateException("生产环境必须配置独立 JWT_SECRET");
        }
    }
}
