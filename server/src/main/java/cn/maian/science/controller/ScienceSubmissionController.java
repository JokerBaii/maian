package cn.maian.science.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.common.api.PageResponse;
import cn.maian.science.dto.CreateScienceSubmissionRequest;
import cn.maian.science.dto.ScienceSubmissionCountResponse;
import cn.maian.science.dto.ScienceSubmissionResponse;
import cn.maian.science.dto.ReviewScienceSubmissionRequest;
import cn.maian.science.service.ScienceSubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

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

    @GetMapping
    public ApiResponse<PageResponse<ScienceSubmissionResponse>> findAll(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(scienceSubmissionService.findAll(pageable)));
    }

    @GetMapping("/approved")
    public ApiResponse<PageResponse<ScienceSubmissionResponse>> findApproved(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(scienceSubmissionService.findApproved(pageable)));
    }

    @GetMapping("/reviews/pending")
    public ApiResponse<PageResponse<ScienceSubmissionResponse>> findPendingReviews(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(scienceSubmissionService.findPendingReviews(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ScienceSubmissionResponse> findById(@PathVariable UUID id) {
        return ApiResponse.ok(scienceSubmissionService.findById(id));
    }

    @PatchMapping("/{id}/review")
    public ApiResponse<ScienceSubmissionResponse> review(
        @PathVariable UUID id,
        @Valid @RequestBody ReviewScienceSubmissionRequest request
    ) {
        return ApiResponse.ok(scienceSubmissionService.review(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        scienceSubmissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
