package cn.maian.device.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.common.api.PageResponse;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.dto.EmergencyDeviceResponse;
import cn.maian.device.dto.SaveEmergencyDeviceRequest;
import cn.maian.device.dto.UpdateDeviceStatusRequest;
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
    public ApiResponse<PageResponse<EmergencyDeviceResponse>> findAll(
        @RequestParam(required = false) DeviceType type,
        Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(emergencyDeviceService.findAll(type, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<EmergencyDeviceResponse> findById(@PathVariable UUID id) {
        return ApiResponse.ok(emergencyDeviceService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmergencyDeviceResponse>> create(
        @Valid @RequestBody SaveEmergencyDeviceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(emergencyDeviceService.create(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<EmergencyDeviceResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody SaveEmergencyDeviceRequest request
    ) {
        return ApiResponse.ok(emergencyDeviceService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<EmergencyDeviceResponse> updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateDeviceStatusRequest request
    ) {
        return ApiResponse.ok(emergencyDeviceService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable UUID id) {
        emergencyDeviceService.delete(id);
        return ApiResponse.ok(true);
    }
}
