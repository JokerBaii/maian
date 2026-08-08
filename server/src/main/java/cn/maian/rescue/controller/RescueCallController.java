package cn.maian.rescue.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.common.api.PageResponse;
import cn.maian.rescue.dto.CreateRescueCallRequest;
import cn.maian.rescue.dto.AttachRescueMediaRequest;
import cn.maian.rescue.dto.LocationUpdateRequest;
import cn.maian.rescue.dto.RequesterRescueResponse;
import cn.maian.rescue.dto.ResponderPresenceRequest;
import cn.maian.rescue.dto.ResponderTaskResponse;
import cn.maian.rescue.service.RescueCallService;
import cn.maian.rescue.service.RescueAttachmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rescue-calls")
public class RescueCallController {

    private final RescueCallService rescueCallService;
    private final RescueAttachmentService rescueAttachmentService;

    public RescueCallController(
        RescueCallService rescueCallService,
        RescueAttachmentService rescueAttachmentService
    ) {
        this.rescueCallService = rescueCallService;
        this.rescueAttachmentService = rescueAttachmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RequesterRescueResponse>> create(
        @Valid @RequestBody CreateRescueCallRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(rescueCallService.create(request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<RequesterRescueResponse> findById(@PathVariable UUID id) {
        return ApiResponse.ok(rescueCallService.findById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<RequesterRescueResponse>> findAll(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(rescueCallService.findAll(pageable)));
    }

    @PostMapping("/{id}/attachments")
    public ApiResponse<RequesterRescueResponse> attach(
        @PathVariable UUID id,
        @Valid @RequestBody AttachRescueMediaRequest request
    ) {
        return ApiResponse.ok(rescueAttachmentService.attach(id, request.mediaId()));
    }

    @PutMapping("/responder-presence")
    public ApiResponse<Void> updateResponderPresence(
        @Valid @RequestBody ResponderPresenceRequest request
    ) {
        rescueCallService.updatePresence(request.latitude(), request.longitude(), request.available());
        return ApiResponse.ok(null);
    }

    @GetMapping("/responder-tasks")
    public ApiResponse<PageResponse<ResponderTaskResponse>> findResponderTasks(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(rescueCallService.findResponderTasks(pageable)));
    }

    @GetMapping("/responder-tasks/{id}")
    public ApiResponse<ResponderTaskResponse> findResponderTask(@PathVariable UUID id) {
        return ApiResponse.ok(rescueCallService.findResponderTask(id));
    }

    @PostMapping("/{id}/acceptance")
    public ApiResponse<ResponderTaskResponse> accept(@PathVariable UUID id) {
        return ApiResponse.ok(rescueCallService.accept(id));
    }

    @PostMapping("/{id}/aed-arrival")
    public ApiResponse<ResponderTaskResponse> arriveAtAed(@PathVariable UUID id) {
        return ApiResponse.ok(rescueCallService.arriveAtAed(id));
    }

    @PostMapping("/{id}/aed-pickup")
    public ApiResponse<ResponderTaskResponse> pickUpAed(@PathVariable UUID id) {
        return ApiResponse.ok(rescueCallService.pickUpAed(id));
    }

    @PostMapping("/{id}/requester-arrival")
    public ApiResponse<ResponderTaskResponse> arriveAtRequester(@PathVariable UUID id) {
        return ApiResponse.ok(rescueCallService.arriveAtRequester(id));
    }

    @PostMapping("/{id}/rescue-start")
    public ApiResponse<ResponderTaskResponse> startRescue(@PathVariable UUID id) {
        return ApiResponse.ok(rescueCallService.startRescue(id));
    }

    @PostMapping("/{id}/completion-submission")
    public ApiResponse<ResponderTaskResponse> submitCompletion(@PathVariable UUID id) {
        return ApiResponse.ok(rescueCallService.submitCompletion(id));
    }

    @PostMapping("/{id}/completion-confirmation")
    public ApiResponse<RequesterRescueResponse> confirmCompletion(@PathVariable UUID id) {
        return ApiResponse.ok(rescueCallService.confirmCompletion(id));
    }

    @PostMapping("/{id}/aed-return")
    public ApiResponse<ResponderTaskResponse> returnAed(@PathVariable UUID id) {
        return ApiResponse.ok(rescueCallService.returnAed(id));
    }

    @PutMapping("/{id}/responder-location")
    public ApiResponse<ResponderTaskResponse> updateResponderLocation(
        @PathVariable UUID id,
        @Valid @RequestBody LocationUpdateRequest request
    ) {
        return ApiResponse.ok(rescueCallService.updateResponderLocation(
            id, request.latitude(), request.longitude()
        ));
    }

    @PostMapping("/{id}/cancellation")
    public ApiResponse<RequesterRescueResponse> cancel(@PathVariable UUID id) {
        return ApiResponse.ok(rescueCallService.cancel(id));
    }
}
