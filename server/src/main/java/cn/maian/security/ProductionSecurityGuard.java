package cn.maian.security;

import cn.maian.config.AuthProperties;
import cn.maian.config.DemoProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ProductionSecurityGuard {

    private static final String DEVELOPMENT_SECRET = "maian-demo-development-secret-change-before-production";

    private final Environment environment;
    private final AuthProperties authProperties;
    private final DemoProperties demoProperties;

    public ProductionSecurityGuard(
        Environment environment,
        AuthProperties authProperties,
        DemoProperties demoProperties
    ) {
        this.environment = environment;
        this.authProperties = authProperties;
        this.demoProperties = demoProperties;
    }

    @PostConstruct
    void validateProductionIsolation() {
        boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!production) {
            return;
        }
        if (demoProperties.enabled()) {
            throw new IllegalStateException("生产环境禁止启用 Demo 身份登录");
        }
        if (DEVELOPMENT_SECRET.equals(authProperties.secret())) {
            throw new IllegalStateException("生产环境必须配置独立 JWT_SECRET");
        }
    }
}
