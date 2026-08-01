package cn.maian.health.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.health.dto.SaveWearableDeviceRequest;
import cn.maian.health.dto.WearableDeviceResponse;
import cn.maian.health.service.WearableDeviceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wearable-device")
public class WearableDeviceController {

    private final WearableDeviceService wearableDeviceService;

    public WearableDeviceController(WearableDeviceService wearableDeviceService) {
        this.wearableDeviceService = wearableDeviceService;
    }

    @GetMapping
    public ApiResponse<WearableDeviceResponse> current() {
        return ApiResponse.ok(wearableDeviceService.current());
    }

    @PutMapping
    public ApiResponse<WearableDeviceResponse> save(
        @Valid @RequestBody SaveWearableDeviceRequest request
    ) {
        return ApiResponse.ok(wearableDeviceService.save(request));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete() {
        wearableDeviceService.delete();
        return ResponseEntity.noContent().build();
    }
}
