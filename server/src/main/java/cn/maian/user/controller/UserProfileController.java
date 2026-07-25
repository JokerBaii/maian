package cn.maian.user.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.user.dto.UserProfileResponse;
import cn.maian.user.dto.VerifyIdentityRequest;
import cn.maian.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ApiResponse<UserProfileResponse> current() {
        return ApiResponse.ok(userProfileService.current());
    }

    @PostMapping("/identity-verification")
    public ApiResponse<UserProfileResponse> verify(
        @Valid @RequestBody VerifyIdentityRequest request
    ) {
        return ApiResponse.ok(userProfileService.verify(request));
    }
}
