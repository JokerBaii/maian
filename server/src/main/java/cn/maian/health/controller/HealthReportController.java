package cn.maian.health.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.health.dto.CreateHealthReportRequest;
import cn.maian.health.dto.HealthReportResponse;
import cn.maian.health.service.HealthReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health-reports")
public class HealthReportController {

    private final HealthReportService healthReportService;

    public HealthReportController(HealthReportService healthReportService) {
        this.healthReportService = healthReportService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HealthReportResponse>> create(
        @Valid @RequestBody CreateHealthReportRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(healthReportService.create(request)));
    }

    @GetMapping
    public ApiResponse<List<HealthReportResponse>> list() {
        return ApiResponse.ok(healthReportService.listCurrentUserReports());
    }

    @GetMapping("/{id}")
    public ApiResponse<HealthReportResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(healthReportService.get(id));
    }
}
