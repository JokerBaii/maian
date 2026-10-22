package cn.maian.agent.controller;

import cn.maian.agent.dto.RescueReviewResponse;
import cn.maian.agent.service.RescueReviewService;
import cn.maian.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rescue-reviews")
public class RescueReviewController {
    private final RescueReviewService service;
    public RescueReviewController(RescueReviewService service) { this.service = service; }
    public record Request(boolean useModel) {}
    @GetMapping("/{id}")
    public ApiResponse<RescueReviewResponse> read(@PathVariable UUID id) { return ApiResponse.ok(service.read(id)); }
    @PostMapping("/{id}")
    public ApiResponse<RescueReviewResponse> request(@PathVariable UUID id, @RequestBody Request request) {
        return ApiResponse.ok(service.request(id, request.useModel()));
    }
}
