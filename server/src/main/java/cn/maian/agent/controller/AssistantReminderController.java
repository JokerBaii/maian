package cn.maian.agent.controller;

import cn.maian.agent.service.AssistantReminderService;
import cn.maian.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/assistant/reminders")
public class AssistantReminderController {
    private final AssistantReminderService service;
    public AssistantReminderController(AssistantReminderService service){this.service=service;}
    public record Create(@NotBlank @Size(max=120) String title,@NotBlank @Pattern(regexp="TIME|DEVICE_REVIEW|SCIENCE_REVIEW") String kind,UUID targetId,@Size(max=50) String dueAt){}
    @PostMapping public ApiResponse<AssistantReminderService.View> create(@Valid @RequestBody Create request){return ApiResponse.ok(service.create(request.title(),request.kind(),request.targetId(),request.dueAt()));}
    @GetMapping public ApiResponse<List<AssistantReminderService.View>> list(){return ApiResponse.ok(service.list());}
    @DeleteMapping("/{id}") public ApiResponse<Boolean> dismiss(@PathVariable UUID id){service.dismiss(id);return ApiResponse.ok(true);}
}
