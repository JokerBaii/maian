package cn.maian.agent;

import cn.maian.agent.repository.RescueReviewRepository;
import cn.maian.agent.service.RescueReviewAgent;
import cn.maian.device.domain.*;
import cn.maian.device.repository.EmergencyDeviceRepository;
import cn.maian.rescue.domain.*;
import cn.maian.rescue.repository.*;
import cn.maian.science.domain.*;
import cn.maian.science.repository.ScienceContentRepository;
import cn.maian.security.JwtService;
import cn.maian.user.domain.UserProfile;
import cn.maian.user.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:rescue-review-http;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop", "app.ai.enabled=false",
    "app.media.directory=${java.io.tmpdir}/maian-review-http-media", "app.agent.initial-delay-ms=3600000",
    "app.dispatch.scheduler-initial-delay-ms=3600000", "app.dispatch.confirmation-initial-delay-ms=3600000",
    "app.agent.follow-up-initial-delay-ms=3600000", "app.agent.review-initial-delay-ms=3600000"
})
@AutoConfigureMockMvc
class RescueReviewHttpIntegrationTest {
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired RescueReviewRepository reviews;
    @Autowired RescueReviewAgent agent;
    @Autowired RescueCallRepository calls;
    @Autowired EmergencyDeviceRepository devices;
    @Autowired RescueEventRepository events;
    @Autowired UserProfileRepository users;
    @Autowired ScienceContentRepository contents;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;

    @Test
    void aggregatesRealEventsMetricsPendingActionsAndArticlesWithoutChangingRescue() throws Exception {
        var f = fixture(true);
        var sequence = calls.findById(f.call()).orElseThrow().getEventSequence();
        http.perform(get(path(f)).header("Authorization", token(f.requester()))).andExpect(status().isOk())
            .andExpect(jsonPath("$.data").doesNotExist());
        request(f);
        agent.process();
        var result = http.perform(get(path(f)).header("Authorization", token(f.requester())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.executionMode").value("RULES"))
            .andExpect(jsonPath("$.data.stale").value(false))
            .andExpect(jsonPath("$.data.data.returnPending").value(true))
            .andExpect(jsonPath("$.data.data.feedbackPending").value(true))
            .andExpect(jsonPath("$.data.data.metrics[0].seconds").value(10))
            .andExpect(jsonPath("$.data.data.metrics[1].seconds").value(80))
            .andExpect(jsonPath("$.data.data.metrics[2].seconds").value(20))
            .andExpect(jsonPath("$.data.data.timeline[0].type").value("CREATED"))
            .andExpect(jsonPath("$.data.articles[*].id").value(org.hamcrest.Matchers.hasItem(f.articleId())))
            .andExpect(jsonPath("$.data.steps[*].module").value(org.hamcrest.Matchers.hasItems("用户请求", "救援事件", "设备与评价", "科普检索", "复盘整理")))
            .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("私密地址", "私密症状", "私密描述", "13912345678");
        assertThat(calls.findById(f.call()).orElseThrow().getEventSequence()).isEqualTo(sequence);
        http.perform(get(path(f)).header("Authorization", token(f.responder()))).andExpect(status().isOk());
        // A fresh report is reused rather than scheduling duplicate model work.
        http.perform(post(path(f)).header("Authorization", token(f.requester()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"useModel\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void returnAndFeedbackMakeOldReportStaleAndRegenerationClosesPendingActions() throws Exception {
        var f = fixture(true);
        request(f); agent.process();
        http.perform(post("/api/v1/rescue-calls/{id}/aed-return", f.call()).header("Authorization", token(f.responder())))
            .andExpect(status().isOk());
        http.perform(get(path(f)).header("Authorization", token(f.requester())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.stale").value(true));
        request(f); agent.process();
        http.perform(get(path(f)).header("Authorization", token(f.requester())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.data.returnPending").value(false));
        http.perform(post("/api/v1/rescue-calls/{id}/feedback", f.call()).header("Authorization", token(f.requester()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5,\"comment\":\"私密评价，不得进入复盘或模型\"}"))
            .andExpect(status().isCreated());
        http.perform(get(path(f)).header("Authorization", token(f.responder())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.stale").value(true));
        request(f); agent.process();
        var response = http.perform(get(path(f)).header("Authorization", token(f.requester())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.stale").value(false))
            .andExpect(jsonPath("$.data.data.returnPending").value(false))
            .andExpect(jsonPath("$.data.data.feedbackPending").value(false)).andReturn();
        assertThat(response.getResponse().getContentAsString()).doesNotContain("私密评价");
    }

    @Test
    void requiresParticipationForReadsAndWritesAndRejectsActiveRescues() throws Exception {
        var ended = fixture(true);
        var active = fixture(false);
        http.perform(get(path(ended))).andExpect(status().isUnauthorized());
        http.perform(post(path(ended)).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        http.perform(get(path(ended)).header("Authorization", token(active.requester()))).andExpect(status().isNotFound());
        http.perform(post(path(ended)).header("Authorization", token(active.responder()))
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound());
        http.perform(post(path(active)).header("Authorization", token(active.requester()))
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isConflict());
        assertThat(reviews.findById(active.call())).isEmpty();
    }

    @Test
    void withdrawnArticleDisappearsFromStoredReport() throws Exception {
        var f = fixture(true);
        request(f); agent.process();
        jdbc.update("update science_contents set active = false where id = ?", f.articleId());
        http.perform(get(path(f)).header("Authorization", token(f.requester())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.articles[*].id")
                .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(f.articleId()))));
    }

    @Test
    void canceledRescueShowsMissingTimesAndDoesNotInventFeedbackOrCareOutcome() throws Exception {
        var f = fixture(false);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var call = calls.findDetailedForUpdateById(f.call()).orElseThrow();
            call.cancelByRequester(f.requester(), Instant.now());
            events.save(new RescueEvent(call.getId(), call.getEventSequence(), RescueEventType.USER_CANCELLED,
                f.requester(), "求救者已取消任务", Instant.now()));
        });
        request(f); agent.process();
        http.perform(get(path(f)).header("Authorization", token(f.requester())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.data.metrics[0].seconds").doesNotExist())
            .andExpect(jsonPath("$.data.data.feedbackPending").value(false))
            .andExpect(jsonPath("$.data.data.returnPending").value(false));
    }

    private void request(Fixture f) throws Exception {
        http.perform(post(path(f)).header("Authorization", token(f.requester()))
            .contentType(MediaType.APPLICATION_JSON).content("{\"useModel\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PENDING"));
    }
    private Fixture fixture(boolean complete) {
        return new TransactionTemplate(transactions).execute(tx -> {
            UUID requester = profile("USER"), responder = profile("VOLUNTEER");
            var submission = ScienceSubmission.create(requester, "已审核 AED 学习", "device", "AED 学习资料", null);
            submission.review(true, "已审核");
            var article = contents.save(ScienceContent.fromApprovedSubmission(submission));
            var device = EmergencyDevice.create(DeviceType.FIXED, "AED", "测试 AED", "私密设备地址", 120.15, 30.28,
                "13912345678", DeviceServiceWindow.alwaysOpen(), null, null, null, null, null, List.of(), List.of());
            device.registerTo(requester); device.review(true, "通过"); device = devices.saveAndFlush(device);
            Instant now = Instant.now().minusSeconds(300);
            var call = RescueCall.create(UrgencyLevel.CRITICAL, 30.28, 120.15, "私密地址", "私密描述",
                Set.of("私密症状"), UUID.randomUUID().toString(), requester, now);
            call = calls.saveAndFlush(call);
            events.save(new RescueEvent(call.getId(), call.getEventSequence(), RescueEventType.CREATED, requester, "请求已创建", now));
            call.beginMatching(now);
            if (complete) {
                device.markReserved(call.getId(), now); call.assignDevice(device, now, 10, 60, "TEST");
                call.acceptBy(responder, now.plusSeconds(10));
                events.save(new RescueEvent(call.getId(), call.getEventSequence(), RescueEventType.ACCEPTED, responder, "施救者已响应", now.plusSeconds(10)));
                call.arriveAtAed(now.plusSeconds(30)); call.pickUpAed(now.plusSeconds(40));
                call.arriveAtRequester(now.plusSeconds(90)); call.startRescue(now.plusSeconds(91));
                call.submitCompletion(now.plusSeconds(180)); call.confirmCompletion(now.plusSeconds(200));
                events.save(new RescueEvent(call.getId(), call.getEventSequence(), RescueEventType.COMPLETION_CONFIRMED, requester, "求救者已确认完成", now.plusSeconds(200)));
            }
            return new Fixture(call.getId(), requester, responder, article.getId());
        });
    }
    private UUID profile(String role) {
        var user = UserProfile.register(UUID.randomUUID(), "复盘测试", "未绑定", Instant.now());
        user.changeRole(role); users.saveAndFlush(user); return user.getId();
    }
    private String path(Fixture f) { return "/api/v1/rescue-reviews/" + f.call(); }
    private String token(UUID user) { return "Bearer " + jwt.issue(user, users.findById(user).orElseThrow().getRole()).accessToken(); }
    private record Fixture(UUID call, UUID requester, UUID responder, String articleId) {}
}
