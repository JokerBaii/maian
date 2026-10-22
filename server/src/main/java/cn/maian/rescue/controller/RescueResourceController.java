package cn.maian.rescue.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.rescue.dto.LocationUpdateRequest;
import cn.maian.rescue.service.AedDispatchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rescue-resources")
public class RescueResourceController {
    private final AedDispatchService dispatch;

    public RescueResourceController(AedDispatchService dispatch) { this.dispatch = dispatch; }

    @PostMapping("/nearby")
    public ApiResponse<List<AedDispatchService.NearbyAed>> nearby(@Valid @RequestBody LocationUpdateRequest location) {
        return ApiResponse.ok(dispatch.findNearby(location.latitude(), location.longitude()));
    }
}
