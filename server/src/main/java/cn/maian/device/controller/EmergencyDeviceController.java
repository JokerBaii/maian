package cn.maian.device.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.common.api.PageResponse;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.dto.PublicEmergencyDeviceResponse;
import cn.maian.device.dto.OwnerEmergencyDeviceResponse;
import cn.maian.device.dto.AdminEmergencyDeviceResponse;
import cn.maian.device.dto.SaveEmergencyDeviceRequest;
import cn.maian.device.dto.ReviewEmergencyDeviceRequest;
import cn.maian.device.dto.UpdateDeviceLocationRequest;
import cn.maian.device.service.EmergencyDeviceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergency-devices")
public class EmergencyDeviceController {

    private final EmergencyDeviceService emergencyDeviceService;

    public EmergencyDeviceController(EmergencyDeviceService emergencyDeviceService) {
        this.emergencyDeviceService = emergencyDeviceService;
    }

    @GetMapping
    public ApiResponse<PageResponse<PublicEmergencyDeviceResponse>> findAll(
        @RequestParam(required = false) DeviceType type,
        Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(emergencyDeviceService.findAll(type, pageable)));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<OwnerEmergencyDeviceResponse>> findMine(
        @RequestParam(required = false) DeviceType type,
        Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(emergencyDeviceService.findMine(type, pageable)));
    }

    @GetMapping("/reviews/pending")
    public ApiResponse<PageResponse<AdminEmergencyDeviceResponse>> findPendingReviews(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(emergencyDeviceService.findPendingReviews(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<PublicEmergencyDeviceResponse> findById(@PathVariable UUID id) {
        return ApiResponse.ok(emergencyDeviceService.findPublicById(id));
    }

    @GetMapping("/mine/{id}")
    public ApiResponse<OwnerEmergencyDeviceResponse> findMineById(@PathVariable UUID id) {
        return ApiResponse.ok(emergencyDeviceService.findMineById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OwnerEmergencyDeviceResponse>> create(
        @Valid @RequestBody SaveEmergencyDeviceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(emergencyDeviceService.create(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<OwnerEmergencyDeviceResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody SaveEmergencyDeviceRequest request
    ) {
        return ApiResponse.ok(emergencyDeviceService.update(id, request));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<OwnerEmergencyDeviceResponse> enable(@PathVariable UUID id) {
        return ApiResponse.ok(emergencyDeviceService.enable(id));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<OwnerEmergencyDeviceResponse> disable(@PathVariable UUID id) {
        return ApiResponse.ok(emergencyDeviceService.disable(id));
    }

    @PatchMapping("/{id}/location")
    public ApiResponse<OwnerEmergencyDeviceResponse> updateLocation(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateDeviceLocationRequest request
    ) {
        return ApiResponse.ok(emergencyDeviceService.updateLocation(id, request));
    }

    @PatchMapping("/{id}/review")
    public ApiResponse<AdminEmergencyDeviceResponse> review(
        @PathVariable UUID id,
        @Valid @RequestBody ReviewEmergencyDeviceRequest request
    ) {
        return ApiResponse.ok(emergencyDeviceService.review(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable UUID id) {
        emergencyDeviceService.delete(id);
        return ApiResponse.ok(true);
    }
}
