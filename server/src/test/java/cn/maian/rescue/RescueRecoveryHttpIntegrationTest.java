package cn.maian.rescue;

import cn.maian.agent.service.CoordinationFollowUpScheduler;
import cn.maian.agent.service.CoordinationQueue;
import cn.maian.agent.service.ResourceCoordinationAgent;
import cn.maian.device.domain.*;
import cn.maian.device.repository.EmergencyDeviceRepository;
import cn.maian.rescue.domain.*;
import cn.maian.rescue.repository.*;
import cn.maian.security.JwtService;
import cn.maian.user.domain.UserProfile;
import cn.maian.user.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:rescue-recovery-http;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop", "app.ai.enabled=false",
    "app.media.directory=${java.io.tmpdir}/maian-recovery-http-media", "app.agent.initial-delay-ms=3600000",
    "app.dispatch.scheduler-initial-delay-ms=3600000", "app.dispatch.confirmation-initial-delay-ms=3600000",
    "app.agent.follow-up-initial-delay-ms=3600000", "app.agent.review-initial-delay-ms=3600000"
})
@AutoConfigureMockMvc
class RescueRecoveryHttpIntegrationTest {
    private static final AtomicInteger AREA = new AtomicInteger();
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired EmergencyDeviceRepository devices;
    @Autowired RescueCallRepository calls;
    @Autowired RescueEventRepository events;
    @Autowired ActiveRescueLockRepository locks;
    @Autowired UserProfileRepository profiles;
    @Autowired JwtService jwt;
    @Autowired PlatformTransactionManager transactions;
    @Autowired CoordinationQueue queue;
    @Autowired ResourceCoordinationAgent agent;
    @Autowired CoordinationFollowUpScheduler followUp;

    @Test
    void nearbyPreviewUsesDispatchEligibilityWithoutCreatingOrReservingResources() throws Exception {
        var f = fixture(false);
        var eligible = addDevice(f, DeviceType.FIXED, f.latitude() + .001, f.requester());
        var reserved = addDevice(f, DeviceType.FIXED, f.latitude() + .0002, f.requester());
        var stale = addDevice(f, DeviceType.MOBILE, f.latitude() + .0001, f.requester());
        addDevice(f, DeviceType.FIXED, f.latitude() + 1, f.requester());
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            devices.findById(reserved.getId()).orElseThrow().markReserved(f.call(), Instant.now());
            devices.findById(stale.getId()).orElseThrow().updateLocation(120.15, f.latitude() + .0001,
                "过期位置", Instant.now().minusSeconds(10000));
        });
        long callsBefore = calls.count();
        for (int i = 0; i < 2; i++) {
            http.perform(post("/api/v1/rescue-resources/nearby").header("Authorization", bearer(f.requester()))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("latitude", f.latitude(), "longitude", 120.15))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(eligible.getId().toString()))
                .andExpect(jsonPath("$.data[0].distanceMeters").isNumber())
                .andExpect(jsonPath("$.data[0].ownerPhone").doesNotExist());
        }
        assertThat(calls.count()).isEqualTo(callsBefore);
        assertThat(devices.findById(eligible.getId()).orElseThrow().getStatus()).isEqualTo(DeviceStatus.AVAILABLE);
        assertThat(devices.findById(eligible.getId()).orElseThrow().getReservedForCallId()).isNull();
    }

    @Test
    void nearbyPreviewRequiresAuthenticationAndValidCoordinates() throws Exception {
        var f = fixture(false);
        http.perform(post("/api/v1/rescue-resources/nearby").contentType(MediaType.APPLICATION_JSON)
            .content("{\"latitude\":30,\"longitude\":120}")).andExpect(status().isUnauthorized());
        for (String body : List.of("{}", "{\"latitude\":91,\"longitude\":120}", "{\"latitude\":30,\"longitude\":181}")) {
            http.perform(post("/api/v1/rescue-resources/nearby").header("Authorization", bearer(f.requester()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void manualReplacementPreservesVolunteerAndExcludesAnotherCarriersMobileAed() throws Exception {
        var f = fixture(false);
        var replacement = addDevice(f, DeviceType.FIXED, f.latitude() + .0027, f.requester());
        var foreignMobile = addDevice(f, DeviceType.MOBILE, f.latitude() + .0001, f.outsider());
        agent.process();
        http.perform(post(path(f, "aed-replacement")).header("Authorization", bearer(f.responder()))
            .contentType(MediaType.APPLICATION_JSON).content(body(f.source())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("EN_ROUTE_TO_AED"))
            .andExpect(jsonPath("$.data.matchedAed.deviceId").value(replacement.getId().toString()))
            .andExpect(jsonPath("$.data.aedCustodyStatus").value("PICKUP_PENDING"))
            .andExpect(jsonPath("$.data.arrivedAtAedAt").doesNotExist());
        var source = devices.findById(f.source()).orElseThrow();
        assertThat(source.getStatus()).isEqualTo(DeviceStatus.DISABLED);
        assertThat(source.getReservedForCallId()).isNull();
        assertThat(devices.findById(foreignMobile.getId()).orElseThrow().getStatus()).isEqualTo(DeviceStatus.AVAILABLE);
        assertThat(calls.findById(f.call()).orElseThrow().getResponderUserId()).isEqualTo(f.responder());
        assertThat(locks.findById(f.requester())).isPresent();
        assertThat(events.findByRescueCallIdAndSequenceGreaterThanOrderBySequenceAsc(f.call(), 0))
            .anySatisfy(event -> {
                assertThat(event.getType()).isEqualTo(RescueEventType.AED_REPLACED);
                assertThat(event.getActorUserId()).isEqualTo(f.responder());
            });
        // A stale retry cannot replace the newly assigned device again.
        http.perform(post(path(f, "aed-replacement")).header("Authorization", bearer(f.responder()))
            .contentType(MediaType.APPLICATION_JSON).content(body(f.source()))).andExpect(status().isConflict());
        followUp.process();
        http.perform(get("/api/v1/coordination-tasks/rescue/{id}", f.call()).header("Authorization", bearer(f.requester())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }

    @Test
    void replacementCanUseTheAssignedVolunteersOwnOnlineMobileAed() throws Exception {
        var f = fixture(false);
        var mobile = addDevice(f, DeviceType.MOBILE, f.latitude() + .0001, f.responder());
        http.perform(post(path(f, "aed-replacement")).header("Authorization", bearer(f.responder()))
            .contentType(MediaType.APPLICATION_JSON).content(body(f.source())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("EN_ROUTE_TO_REQUESTER"))
            .andExpect(jsonPath("$.data.matchedAed.deviceId").value(mobile.getId().toString()))
            .andExpect(jsonPath("$.data.aedCustodyStatus").value("IN_CUSTODY"));
    }

    @Test
    void noReplacementKeepsOriginalReservationUntilHumanEndsDispatchAndRequesterCanCallAgain() throws Exception {
        var f = fixture(false);
        agent.process();
        http.perform(post(path(f, "aed-replacement")).header("Authorization", bearer(f.responder()))
            .contentType(MediaType.APPLICATION_JSON).content(body(f.source()))).andExpect(status().isConflict());
        assertThat(devices.findById(f.source()).orElseThrow().getReservedForCallId()).isEqualTo(f.call());
        http.perform(post(path(f, "resource-failure")).header("Authorization", bearer(f.responder()))
            .contentType(MediaType.APPLICATION_JSON).content(body(f.source())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SYSTEM_FAILED"));
        assertThat(locks.findById(f.requester())).isEmpty();
        assertThat(devices.findById(f.source()).orElseThrow().getStatus()).isEqualTo(DeviceStatus.DISABLED);
        assertThat(devices.findById(f.source()).orElseThrow().getReservedForCallId()).isNull();
        followUp.process();
        http.perform(get("/api/v1/coordination-tasks/rescue/{id}", f.call()).header("Authorization", bearer(f.requester())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
        var response = http.perform(post("/api/v1/rescue-calls").header("Authorization", bearer(f.requester()))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "urgency", "CRITICAL", "latitude", f.latitude(), "longitude", 120.15, "address", "测试现场",
                "symptoms", List.of("测试症状"), "clientRequestId", UUID.randomUUID().toString()))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("MATCHING")).andReturn();
        assertThat(json.readTree(response.getResponse().getContentAsString()).path("data").path("id").asText())
            .isNotEqualTo(f.call().toString());
    }

    @Test
    void onlyAssignedVolunteerCanRecoverAndExpectedDeviceIsValidated() throws Exception {
        var f = fixture(false);
        for (String action : List.of("aed-replacement", "resource-failure")) {
            http.perform(post(path(f, action)).contentType(MediaType.APPLICATION_JSON).content(body(f.source())))
                .andExpect(status().isUnauthorized());
            http.perform(post(path(f, action)).header("Authorization", bearer(f.requester()))
                .contentType(MediaType.APPLICATION_JSON).content(body(f.source()))).andExpect(status().isForbidden());
            http.perform(post(path(f, action)).header("Authorization", bearer(f.outsider()))
                .contentType(MediaType.APPLICATION_JSON).content(body(f.source()))).andExpect(status().isConflict());
            http.perform(post(path(f, action)).header("Authorization", bearer(f.responder()))
                .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID()))).andExpect(status().isConflict());
            http.perform(post(path(f, action)).header("Authorization", bearer(f.responder()))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        }
        assertThat(devices.findById(f.source()).orElseThrow().getReservedForCallId()).isEqualTo(f.call());
    }

    @Test
    void recoveryNeverReleasesAnAedThatHasAlreadyBeenPickedUp() throws Exception {
        var f = fixture(true);
        for (String action : List.of("aed-replacement", "resource-failure")) {
            http.perform(post(path(f, action)).header("Authorization", bearer(f.responder()))
                .contentType(MediaType.APPLICATION_JSON).content(body(f.source()))).andExpect(status().isConflict());
        }
        assertThat(devices.findById(f.source()).orElseThrow().getReservedForCallId()).isEqualTo(f.call());
        assertThat(calls.findById(f.call()).orElseThrow().getAedCustodyStatus()).isEqualTo(AedCustodyStatus.IN_CUSTODY);
    }

    @Test
    void repairedDeviceCannotBeEndedOrReplacedByAnOldFaultConfirmation() throws Exception {
        var f = fixture(false);
        http.perform(post("/api/v1/emergency-devices/{id}/enable", f.source())
            .header("Authorization", bearer(f.requester()))).andExpect(status().isOk());
        for (String action : List.of("aed-replacement", "resource-failure")) {
            http.perform(post(path(f, action)).header("Authorization", bearer(f.responder()))
                .contentType(MediaType.APPLICATION_JSON).content(body(f.source()))).andExpect(status().isConflict());
        }
        assertThat(devices.findById(f.source()).orElseThrow().getStatus()).isEqualTo(DeviceStatus.RESERVED);
        assertThat(locks.findById(f.requester())).isPresent();
    }

    private Fixture fixture(boolean pickedUp) {
        return new TransactionTemplate(transactions).execute(tx -> {
            UUID requester = profile("USER"), responder = profile("VOLUNTEER"), outsider = profile("VOLUNTEER");
            double latitude = 20 + AREA.incrementAndGet();
            var source = newDevice(DeviceType.FIXED, latitude + .0001, requester);
            var now = Instant.now();
            var call = RescueCall.create(UrgencyLevel.CRITICAL, latitude, 120.15, "测试现场", null,
                Set.of("测试症状"), UUID.randomUUID().toString(), requester, now);
            call.beginMatching(now); source.markReserved(call.getId(), now);
            call.assignDevice(source, now, 10, 60, "TEST"); call.acceptBy(responder, now);
            if (pickedUp) { call.arriveAtAed(now); call.pickUpAed(now); }
            calls.saveAndFlush(call);
            source.reportFault(); queue.enqueue(source, "FAULT_REPORTED");
            locks.save(new ActiveRescueLock(requester, call.getId(), now));
            return new Fixture(call.getId(), source.getId(), requester, responder, outsider, latitude);
        });
    }

    private UUID profile(String role) {
        var profile = UserProfile.register(UUID.randomUUID(), "接口测试", "未绑定", Instant.now());
        profile.changeRole(role); profiles.saveAndFlush(profile); return profile.getId();
    }

    private EmergencyDevice addDevice(Fixture f, DeviceType type, double latitude, UUID owner) {
        return new TransactionTemplate(transactions).execute(tx -> newDevice(type, latitude, owner));
    }

    private EmergencyDevice newDevice(DeviceType type, double latitude, UUID owner) {
        var device = EmergencyDevice.create(type, "AED", "测试 AED", "测试设备地址", 120.15, latitude,
            null, DeviceServiceWindow.alwaysOpen(), null, null, type == DeviceType.MOBILE ? "测试车辆" : null,
            type == DeviceType.MOBILE ? 5 : null, null, List.of(), List.of());
        device.registerTo(owner); device.review(true, "测试通过");
        if (type == DeviceType.MOBILE) device.updateLocation(120.15, latitude, "测试设备地址", Instant.now());
        return devices.saveAndFlush(device);
    }

    private String bearer(UUID id) { return "Bearer " + jwt.issue(id, profiles.findById(id).orElseThrow().getRole()).accessToken(); }
    private String path(Fixture f, String action) { return "/api/v1/rescue-calls/" + f.call() + "/" + action; }
    private String body(UUID deviceId) throws Exception { return json.writeValueAsString(Map.of("expectedDeviceId", deviceId)); }
    private record Fixture(UUID call, UUID source, UUID requester, UUID responder, UUID outsider, double latitude) {}
}
