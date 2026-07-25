package cn.maian.health.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.health.dto.HealthMonitoringResponse;
import cn.maian.health.service.HealthMonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health-monitoring")
public class HealthMonitoringController {

    private final HealthMonitoringService healthMonitoringService;

    public HealthMonitoringController(HealthMonitoringService healthMonitoringService) {
        this.healthMonitoringService = healthMonitoringService;
    }

    @GetMapping
    public ApiResponse<HealthMonitoringResponse> currentSummary() {
        return ApiResponse.ok(healthMonitoringService.currentSummary());
    }
}
