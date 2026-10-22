package cn.maian.agent;

import cn.maian.agent.service.AssistantReminderService;
import cn.maian.agent.service.AssistantReminderScheduler;
import cn.maian.device.domain.*;
import cn.maian.device.repository.EmergencyDeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.*;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:assistant-reminders;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop","app.ai.enabled=false","app.media.directory=${java.io.tmpdir}/maian-reminder-test-media","app.agent.reminder-initial-delay-ms=3600000","app.dispatch.scheduler-initial-delay-ms=3600000","app.dispatch.confirmation-initial-delay-ms=3600000","app.agent.initial-delay-ms=3600000","app.agent.review-initial-delay-ms=3600000","app.agent.follow-up-initial-delay-ms=3600000"})
@AutoConfigureMockMvc
@Import(AssistantReminderHttpIntegrationTest.Time.class)
class AssistantReminderHttpIntegrationTest {
    static class MutableClock extends Clock {Instant now=Instant.now();public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}}
    @TestConfiguration static class Time {@Bean @Primary MutableClock testClock(){return new MutableClock();}}
    @Autowired MockMvc http;@Autowired ObjectMapper json;@Autowired MutableClock clock;@Autowired AssistantReminderService service;@Autowired AssistantReminderScheduler scheduler;@Autowired EmergencyDeviceRepository devices;
    @BeforeEach void reset(){clock.now=Instant.now();}
    @Test void timedReminderPersistsTriggersAndCannotBeReadOrDismissedByAnotherAccount()throws Exception{
        String owner=register("13900000111"),other=register("13900000112");
        String id=create(owner,Map.of("title","归还AED","kind","TIME","dueAt",clock.now.plusSeconds(120).toString()));
        service.check(UUID.fromString(id));read(owner).andExpect(jsonPath("$.data[0].status").value("WAITING"));
        read(other).andExpect(jsonPath("$.data").isEmpty());
        http.perform(delete("/api/v1/assistant/reminders/"+id).header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
        clock.now=clock.now.plusSeconds(180);scheduler.process();
        read(owner).andExpect(jsonPath("$.data[0].status").value("READY")).andExpect(jsonPath("$.data[0].result").value("到提醒时间了：归还AED"));
        http.perform(delete("/api/v1/assistant/reminders/"+id).header("Authorization","Bearer "+owner)).andExpect(status().isOk());
        service.check(UUID.fromString(id));read(owner).andExpect(jsonPath("$.data").isEmpty());
    }
    @Test void reviewFollowUpChecksActualOwnedStateAndContinuesAfterTheConversationCloses()throws Exception{
        String owner=register("13900000113"),other=register("13900000114");
        UUID user=UUID.fromString(json.readTree(Base64.getUrlDecoder().decode(owner.split("\\.")[1])).path("sub").asText());
        var device=EmergencyDevice.create(DeviceType.FIXED,"AED","待审核设备","测试位置",120.15,30.28,null,DeviceServiceWindow.alwaysOpen(),null,null,null,null,null,List.of(),List.of());device.registerTo(user);devices.saveAndFlush(device);
        http.perform(post("/api/v1/assistant/reminders").header("Authorization","Bearer "+other).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("title","审核跟进","kind","DEVICE_REVIEW","targetId",device.getId())))).andExpect(status().isNotFound());
        String id=create(owner,Map.of("title","设备审核跟进","kind","DEVICE_REVIEW","targetId",device.getId()));
        service.check(UUID.fromString(id));read(owner).andExpect(jsonPath("$.data[0].status").value("WAITING"));
        var reviewed=devices.findById(device.getId()).orElseThrow();reviewed.review(true,"通过");devices.saveAndFlush(reviewed);clock.now=clock.now.plusSeconds(31);scheduler.process();
        read(owner).andExpect(jsonPath("$.data[0].status").value("READY")).andExpect(jsonPath("$.data[0].result").value("设备审核状态已更新：已通过"));
    }
    @Test void rejectsMissingPastAndUnownedReminderParameters()throws Exception{
        String owner=register("13900000115");
        for(var body:List.of(Map.of("title","测试","kind","TIME","dueAt","随便"),Map.of("title","测试","kind","TIME","dueAt",clock.now.minusSeconds(1).toString()),Map.of("title","测试","kind","DEVICE_REVIEW"))){
            http.perform(post("/api/v1/assistant/reminders").header("Authorization","Bearer "+owner).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().is4xxClientError());
        }
        http.perform(get("/api/v1/assistant/reminders")).andExpect(status().isUnauthorized());
    }
    private org.springframework.test.web.servlet.ResultActions read(String token)throws Exception{return http.perform(get("/api/v1/assistant/reminders").header("Authorization","Bearer "+token)).andExpect(status().isOk());}
    private String create(String token,Map<String,?> data)throws Exception{var r=http.perform(post("/api/v1/assistant/reminders").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(data))).andExpect(status().isOk()).andReturn();return json.readTree(r.getResponse().getContentAsString()).path("data").path("id").asText();}
    private String register(String phone)throws Exception{var r=http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("phone",phone,"password","reminder-test-password","nickname","提醒测试")))).andExpect(status().isCreated()).andReturn();return json.readTree(r.getResponse().getContentAsString()).path("data").path("accessToken").asText();}
}
