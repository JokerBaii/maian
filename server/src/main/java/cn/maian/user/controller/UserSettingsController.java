package cn.maian.user.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.user.dto.UpdateUserSettingsRequest;
import cn.maian.user.dto.UserSettingsResponse;
import cn.maian.user.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @GetMapping
    public ApiResponse<UserSettingsResponse> current() {
        return ApiResponse.ok(userSettingsService.current());
    }

    @PutMapping
    public ApiResponse<UserSettingsResponse> update(
        @Valid @RequestBody UpdateUserSettingsRequest request
    ) {
        return ApiResponse.ok(userSettingsService.update(request));
    }
}
