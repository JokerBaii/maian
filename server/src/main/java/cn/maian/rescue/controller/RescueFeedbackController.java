package cn.maian.rescue.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.rescue.dto.CreateRescueFeedbackRequest;
import cn.maian.rescue.dto.RescueFeedbackResponse;
import cn.maian.rescue.service.RescueFeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rescue-calls/{rescueCallId}/feedback")
public class RescueFeedbackController {

    private final RescueFeedbackService rescueFeedbackService;

    public RescueFeedbackController(RescueFeedbackService rescueFeedbackService) {
        this.rescueFeedbackService = rescueFeedbackService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RescueFeedbackResponse>> submit(
        @PathVariable UUID rescueCallId,
        @Valid @RequestBody CreateRescueFeedbackRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(rescueFeedbackService.submit(rescueCallId, request)));
    }

    @GetMapping
    public ApiResponse<RescueFeedbackResponse> get(@PathVariable UUID rescueCallId) {
        return ApiResponse.ok(rescueFeedbackService.getForRescue(rescueCallId));
    }
}
