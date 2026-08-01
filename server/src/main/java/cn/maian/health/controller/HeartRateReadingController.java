package cn.maian.health.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.health.dto.CreateHeartRateReadingRequest;
import cn.maian.health.dto.HeartRateReadingResponse;
import cn.maian.health.service.HeartRateReadingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/heart-rate-readings")
public class HeartRateReadingController {

    private final HeartRateReadingService heartRateReadingService;

    public HeartRateReadingController(HeartRateReadingService heartRateReadingService) {
        this.heartRateReadingService = heartRateReadingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HeartRateReadingResponse>> create(
        @Valid @RequestBody CreateHeartRateReadingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(heartRateReadingService.create(request)));
    }
}
