package cn.maian.rescue.controller;

import cn.maian.rescue.service.RescueRealtimeHub;
import cn.maian.user.service.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/rescue-events")
public class RescueRealtimeController {
    private final RescueRealtimeHub rescueRealtimeHub;
    private final CurrentUserService currentUserService;

    public RescueRealtimeController(
        RescueRealtimeHub rescueRealtimeHub,
        CurrentUserService currentUserService
    ) {
        this.rescueRealtimeHub = rescueRealtimeHub;
        this.currentUserService = currentUserService;
    }

    @GetMapping(path = "/stream", produces = "text/event-stream")
    public SseEmitter stream() {
        var profile = currentUserService.currentProfile();
        return rescueRealtimeHub.subscribe(profile.getId(), profile.getRole());
    }
}
