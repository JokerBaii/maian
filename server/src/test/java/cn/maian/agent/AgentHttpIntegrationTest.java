package cn.maian.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.maian.agent.service.ResourceCoordinationAgent;
import cn.maian.device.domain.DeviceServiceWindow;
import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.EmergencyDevice;
import cn.maian.device.repository.EmergencyDeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:agent-http-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.ai.enabled=false", "app.media.directory=${java.io.tmpdir}/maian-agent-smoke-media",
    "app.agent.initial-delay-ms=3600000", "app.dispatch.scheduler-initial-delay-ms=3600000",
    "app.dispatch.confirmation-initial-delay-ms=3600000", "app.agent.follow-up-initial-delay-ms=3600000"
})
@AutoConfigureMockMvc
class AgentHttpIntegrationTest {
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired EmergencyDeviceRepository devices;
    @Autowired ResourceCoordinationAgent agent;

    @Test
    void requiresAuthenticationForCoordinationAndAssistant() throws Exception {
        http.perform(get("/api/v1/coordination-tasks/mine")).andExpect(status().isUnauthorized());
        http.perform(post("/api/v1/assistant").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\":\"隐私设置\",\"useModel\":false}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void registeredUserCanLocateSettingsThroughRealSecurityAndApplicationWiring() throws Exception {
        var registration = http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"phone\":\"13900000001\",\"password\":\"agent-test-password\",\"nickname\":\"接口测试用户\"}"))
            .andExpect(status().isCreated()).andReturn();
        String token = json.readTree(registration.getResponse().getContentAsString()).path("data").path("accessToken").asText();
        http.perform(post("/api/v1/assistant").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"在哪里改心率预警阈值\",\"useModel\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.executionMode").value("RULES"))
            .andExpect(jsonPath("$.data.actions[0].url").value("/pages/mine/settings?section=thresholds"));
        http.perform(get("/api/v1/coordination-tasks/mine").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void faultEndpointEnqueuesCommittedWorkAndAgentUpdatesRescueVisibleOnlyToParticipants() throws Exception {
        String ownerToken = register("13900000002");
        String requesterToken = register("13900000003");
        String outsiderToken = register("13900000004");
        String subject = json.readTree(Base64.getUrlDecoder().decode(ownerToken.split("\\.")[1]))
            .path("sub").asText();
        var original = device(UUID.fromString(subject), "故障 AED", 30.2801);
        var replacement = device(UUID.fromString(subject), "替代 AED", 30.2810);
        var creation = http.perform(post("/api/v1/rescue-calls").header("Authorization", "Bearer " + requesterToken)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"urgency":"CRITICAL","latitude":30.28,"longitude":120.15,"address":"测试现场",
                 "symptoms":["测试症状"],"clientRequestId":"agent-integration-rescue"}
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.matchedAed.deviceId").value(original.getId().toString())).andReturn();
        String callId = json.readTree(creation.getResponse().getContentAsString()).path("data").path("id").asText();
        http.perform(post("/api/v1/emergency-devices/{id}/fault", original.getId())
            .header("Authorization", "Bearer " + outsiderToken)).andExpect(status().isNotFound());
        http.perform(post("/api/v1/emergency-devices/{id}/fault", original.getId())
            .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DISABLED"))
            .andExpect(jsonPath("$.data.hasActiveReservation").value(true));
        // The request transaction has committed; execute the actual scheduled agent bean afterwards.
        agent.process();
        http.perform(get("/api/v1/rescue-calls/{id}", callId).header("Authorization", "Bearer " + requesterToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.matchedAed.deviceId").value(replacement.getId().toString()));
        http.perform(get("/api/v1/coordination-tasks/rescue/{id}", callId)
            .header("Authorization", "Bearer " + requesterToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].status").value("COMPLETED"))
            .andExpect(jsonPath("$.data[0].executionMode").value("RULES"))
            .andExpect(jsonPath("$.data[0].steps[*].module").value(org.hamcrest.Matchers.hasItems(
                "设备管理", "救援任务", "资源调度", "协同执行")));
        http.perform(get("/api/v1/coordination-tasks/rescue/{id}", callId)
            .header("Authorization", "Bearer " + outsiderToken)).andExpect(status().isNotFound());
        http.perform(get("/api/v1/coordination-tasks/mine").header("Authorization", "Bearer " + outsiderToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }

    private String register(String phone) throws Exception {
        var response = http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of("phone", phone, "password", "agent-test-password", "nickname", "联调用户"))))
            .andExpect(status().isCreated()).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    private EmergencyDevice device(UUID owner, String name, double latitude) {
        var device = EmergencyDevice.create(DeviceType.FIXED, "AED", name, "测试设备地址", 120.15, latitude,
            null, DeviceServiceWindow.alwaysOpen(), null, null, null, null, null, List.of(), List.of());
        device.registerTo(owner); device.review(true, "测试审核通过");
        return devices.saveAndFlush(device);
    }
}
