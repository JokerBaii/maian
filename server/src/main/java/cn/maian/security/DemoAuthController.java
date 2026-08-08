package cn.maian.security;

import cn.maian.common.api.ApiResponse;
import cn.maian.security.dto.AuthTokenResponse;
import cn.maian.security.dto.DemoLoginRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
@RequestMapping("/api/v1/auth")
public class DemoAuthController {

    private final DemoAuthService demoAuthService;

    public DemoAuthController(DemoAuthService demoAuthService) {
        this.demoAuthService = demoAuthService;
    }

    @PostMapping("/demo")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody DemoLoginRequest request) {
        return ApiResponse.ok(demoAuthService.login(request.userId()));
    }
}
