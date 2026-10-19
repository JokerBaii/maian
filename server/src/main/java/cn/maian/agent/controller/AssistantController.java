package cn.maian.agent.controller;

import cn.maian.agent.dto.AssistantResponse;
import cn.maian.agent.service.AssistantService;
import cn.maian.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {
    private final AssistantService service;
    public AssistantController(AssistantService service) { this.service = service; }
    public record Request(@NotBlank @Size(max = 300) String message, boolean useModel) {}
    @PostMapping
    public ApiResponse<AssistantResponse> assist(@Valid @RequestBody Request request) {
        return ApiResponse.ok(service.assist(request.message(), request.useModel()));
    }
}
