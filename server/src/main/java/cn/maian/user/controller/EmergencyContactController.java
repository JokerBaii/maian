package cn.maian.user.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.user.dto.EmergencyContactResponse;
import cn.maian.user.dto.SaveEmergencyContactRequest;
import cn.maian.user.service.EmergencyContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergency-contacts")
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;

    public EmergencyContactController(EmergencyContactService emergencyContactService) {
        this.emergencyContactService = emergencyContactService;
    }

    @GetMapping
    public ApiResponse<List<EmergencyContactResponse>> findAll() {
        return ApiResponse.ok(emergencyContactService.findAll());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmergencyContactResponse>> create(
        @Valid @RequestBody SaveEmergencyContactRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(emergencyContactService.create(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<EmergencyContactResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody SaveEmergencyContactRequest request
    ) {
        return ApiResponse.ok(emergencyContactService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable UUID id) {
        emergencyContactService.delete(id);
        return ApiResponse.ok(true);
    }
}
