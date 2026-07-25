package cn.maian.science.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.science.dto.CreateScienceSubmissionRequest;
import cn.maian.science.dto.ScienceSubmissionCountResponse;
import cn.maian.science.dto.ScienceSubmissionResponse;
import cn.maian.science.service.ScienceSubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/science-submissions")
public class ScienceSubmissionController {

    private final ScienceSubmissionService scienceSubmissionService;

    public ScienceSubmissionController(ScienceSubmissionService scienceSubmissionService) {
        this.scienceSubmissionService = scienceSubmissionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScienceSubmissionResponse>> create(
        @Valid @RequestBody CreateScienceSubmissionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(scienceSubmissionService.create(request)));
    }

    @GetMapping("/count")
    public ApiResponse<ScienceSubmissionCountResponse> count() {
        return ApiResponse.ok(scienceSubmissionService.countCurrentUserSubmissions());
    }
}
