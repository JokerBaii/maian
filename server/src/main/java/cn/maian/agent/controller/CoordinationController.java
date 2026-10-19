package cn.maian.agent.controller;

import cn.maian.agent.dto.CoordinationTaskResponse;
import cn.maian.agent.service.ResourceCoordinationService;
import cn.maian.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coordination-tasks")
public class CoordinationController {
    private final ResourceCoordinationService service;
    public CoordinationController(ResourceCoordinationService service) { this.service = service; }
    @GetMapping("/mine")
    public ApiResponse<List<CoordinationTaskResponse>> mine() { return ApiResponse.ok(service.mine()); }
    @PostMapping("/{id}/retry")
    public ApiResponse<Boolean> retry(@PathVariable UUID id) { service.retry(id); return ApiResponse.ok(true); }
    @GetMapping("/rescue/{id}")
    public ApiResponse<List<CoordinationTaskResponse>> forRescue(@PathVariable UUID id) {
        return ApiResponse.ok(service.forRescue(id));
    }
}
