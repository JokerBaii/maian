package cn.maian.rescue.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.common.api.PageResponse;
import cn.maian.rescue.dto.CreateRescueCallRequest;
import cn.maian.rescue.dto.RescueCallResponse;
import cn.maian.rescue.dto.UpdateRescueStatusRequest;
import cn.maian.rescue.service.RescueCallService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rescue-calls")
public class RescueCallController {

    private final RescueCallService rescueCallService;

    public RescueCallController(RescueCallService rescueCallService) {
        this.rescueCallService = rescueCallService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RescueCallResponse>> create(
        @Valid @RequestBody CreateRescueCallRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(rescueCallService.create(request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<RescueCallResponse> findById(@PathVariable UUID id) {
        return ApiResponse.ok(rescueCallService.findById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<RescueCallResponse>> findAll(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(rescueCallService.findAll(pageable)));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<RescueCallResponse> updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateRescueStatusRequest request
    ) {
        return ApiResponse.ok(rescueCallService.updateStatus(id, request.status()));
    }
}
