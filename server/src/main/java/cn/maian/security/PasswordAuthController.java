package cn.maian.security;

import cn.maian.common.api.ApiResponse;
import cn.maian.security.dto.AuthTokenResponse;
import cn.maian.security.dto.LoginRequest;
import cn.maian.security.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class PasswordAuthController {
    private final PasswordAuthService passwordAuthService;

    public PasswordAuthController(PasswordAuthService passwordAuthService) {
        this.passwordAuthService = passwordAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(passwordAuthService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> register(
        @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(passwordAuthService.register(request)));
    }
}
