import cn.maian.MaianServerApplication;
import cn.maian.agent.domain.CoordinationTask;
import cn.maian.agent.repository.CoordinationTaskRepository;
import cn.maian.device.domain.*;
import cn.maian.device.repository.EmergencyDeviceRepository;
import cn.maian.rescue.domain.*;
import cn.maian.rescue.repository.*;
import cn.maian.science.domain.*;
import cn.maian.science.repository.*;
import cn.maian.security.*;
import cn.maian.security.dto.RegisterRequest;
import cn.maian.user.domain.UserProfile;
import cn.maian.user.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Standalone source launcher: excluded from the production Maven artifact. */
public class PreviewServer {
    public static void main(String[] args) {
        SpringApplication.run(new Class<?>[]{MaianServerApplication.class, PreviewData.class}, args);
    }

    @Configuration(proxyBeanMethods = false)
    public static class PreviewData {
        @Bean
        ApplicationRunner seedPreviewData(DataSource database, Environment env,
                PlatformTransactionManager transactions, PasswordAuthService auth,
                UserCredentialRepository credentials, UserProfileRepository users,
                EmergencyDeviceRepository devices, RescueCallRepository calls,
                RescueEventRepository events, ScienceSubmissionRepository submissions,
                ScienceContentRepository contents, CoordinationTaskRepository tasks,
                ObjectMapper json) {
            return args -> {
                // Refuse to seed a MySQL or any other non-preview database, even if misconfigured.
                Path directory = Path.of(env.getRequiredProperty("app.preview.directory")).toRealPath();
                String expectedUrl = "jdbc:h2:file:" + directory.resolve("preview");
                try (var connection = database.getConnection()) {
                    if (!connection.getMetaData().getURL().equals(expectedUrl)) {
                        throw new IllegalStateException("演示启动器仅支持专用的本地 H2 preview 数据库");
                    }
                }
                String password = env.getRequiredProperty("app.preview.password");
                if (password.length() < 12 || password.length() > 72) {
                    throw new IllegalStateException("演示密码长度必须为 12 至 72 位");
                }
                Map<String, String> manifest = new TransactionTemplate(transactions).execute(tx -> {
                    if (credentials.findByPhone("19900000001").isPresent()) return null;
                    UserProfile requester = account(auth, credentials, users, "19900000001", "演示求救者", "USER", password);
                    UserProfile responder = account(auth, credentials, users, "19900000002", "演示志愿者", "VOLUNTEER", password);
                    account(auth, credentials, users, "19900000003", "演示管理员", "ADMIN", password);

                    var returningAed = device(requester.getId(), "【演示】待归还 AED", 120.1500, 30.2800, true, devices);
                    device(requester.getId(), "【演示】备用 AED A", 120.1510, 30.2810, true, devices);
                    device(requester.getId(), "【演示】备用 AED B", 120.1520, 30.2790, true, devices);
                    device(requester.getId(), "【演示】待审核 AED", 120.1530, 30.2820, false, devices);
                    var faultyAed = device(requester.getId(), "【演示】故障维护 AED", 120.1540, 30.2830, true, devices);
                    faultyAed.reportFault();
                    tasks.save(CoordinationTask.create(faultyAed, "FAULT_REPORTED", "preview-fault-v1", Instant.now()));

                    Instant start = Instant.now().minusSeconds(900);
                    var call = calls.saveAndFlush(RescueCall.create(UrgencyLevel.CRITICAL, 30.2805, 120.1505,
                        "【演示】测试场景集合点", "虚构的比赛演示记录，用于体验救援复盘与待办。",
                        Set.of(), "preview-completed-v1", requester.getId(), start));
                    event(events, call, RescueEventType.CREATED, requester.getId(), "【演示】呼救请求已创建", start);
                    call.beginMatching(start.plusSeconds(1));
                    event(events, call, RescueEventType.MATCHING_STARTED, null, "开始检查附近 AED", start.plusSeconds(1));
                    returningAed.markReserved(call.getId(), start.plusSeconds(2));
                    call.assignDevice(returningAed, start.plusSeconds(2), 80, 60, "FIXED_RUNNER");
                    event(events, call, RescueEventType.AED_ASSIGNED, null, "已预留演示固定 AED", start.plusSeconds(2));
                    call.acceptBy(responder.getId(), start.plusSeconds(20));
                    event(events, call, RescueEventType.ACCEPTED, responder.getId(), "志愿者已响应", start.plusSeconds(20));
                    call.arriveAtAed(start.plusSeconds(60));
                    event(events, call, RescueEventType.ARRIVED_AT_AED, responder.getId(), "已到达 AED 取用点", start.plusSeconds(60));
                    call.pickUpAed(start.plusSeconds(80));
                    event(events, call, RescueEventType.AED_PICKED_UP, responder.getId(), "已取用 AED", start.plusSeconds(80));
                    call.arriveAtRequester(start.plusSeconds(150));
                    event(events, call, RescueEventType.ARRIVED_AT_REQUESTER, responder.getId(), "已到达演示现场", start.plusSeconds(150));
                    call.startRescue(start.plusSeconds(160));
                    event(events, call, RescueEventType.RESCUE_STARTED, responder.getId(), "演示救援流程开始", start.plusSeconds(160));
                    call.submitCompletion(start.plusSeconds(300));
                    event(events, call, RescueEventType.COMPLETION_SUBMITTED, responder.getId(), "已提交完成确认", start.plusSeconds(300));
                    call.confirmCompletion(start.plusSeconds(330));
                    event(events, call, RescueEventType.COMPLETION_CONFIRMED, requester.getId(), "已确认完成，设备待归还", start.plusSeconds(330));

                    var canceled = calls.saveAndFlush(RescueCall.create(UrgencyLevel.CRITICAL, 30.2805, 120.1505,
                        "【演示】测试场景集合点", "虚构的取消记录，用于检查缺失时间的展示。", Set.of(),
                        "preview-canceled-v1", requester.getId(), start.minusSeconds(600)));
                    event(events, canceled, RescueEventType.CREATED, requester.getId(), "【演示】请求已创建", start.minusSeconds(600));
                    canceled.cancelByRequester(requester.getId(), start.minusSeconds(580));
                    event(events, canceled, RescueEventType.USER_CANCELLED, requester.getId(), "求救者已取消演示任务", start.minusSeconds(580));

                    for (var title : List.of("【演示】AED 取用与归还记录", "【演示】设备故障如何上报", "【演示】救援结束后查看复盘")) {
                        var submission = ScienceSubmission.create(requester.getId(), title, "device",
                            "这是用于验证文章检索和推荐入口的演示内容。可在救援详情查看事件时间线、待办和执行过程；设备操作请从对应业务入口完成。", null);
                        submission.review(true, "本地演示样例");
                        submissions.save(submission);
                        contents.save(ScienceContent.fromApprovedSubmission(submission));
                    }
                    submissions.save(ScienceSubmission.create(requester.getId(), "【演示】待审核的设备管理经验", "device",
                        "用于展示管理员审核流程的演示投稿。", null));
                    return Map.of("completedRescueId", call.getId().toString(), "canceledRescueId", canceled.getId().toString(),
                        "requesterPhone", "19900000001", "volunteerPhone", "19900000002", "adminPhone", "19900000003");
                });
                if (manifest != null) {
                    json.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("demo.json").toFile(), manifest);
                    System.out.println("演示数据已创建；救援详情 ID：" + manifest.get("completedRescueId"));
                } else {
                    System.out.println("复用现有演示数据，不重复初始化；重置方式见 docs/DEMO_ENVIRONMENT.md。");
                }
            };
        }

        private static UserProfile account(PasswordAuthService auth, UserCredentialRepository credentials,
                UserProfileRepository users, String phone, String nickname, String role, String password) {
            auth.register(new RegisterRequest(phone, password, nickname));
            var profile = users.findById(credentials.findByPhone(phone).orElseThrow().getUserId()).orElseThrow();
            profile.changeRole(role);
            return profile;
        }

        private static EmergencyDevice device(UUID owner, String name, double longitude, double latitude,
                boolean approved, EmergencyDeviceRepository devices) {
            var device = EmergencyDevice.create(DeviceType.FIXED, "AED", name, "【演示】虚构设备位置", longitude, latitude,
                "19900000001", DeviceServiceWindow.alwaysOpen(), null, "演示设备管理方", null, null,
                "仅用于本地比赛演示", List.of(), List.of());
            device.registerTo(owner);
            if (approved) device.review(true, "本地演示审核");
            return devices.saveAndFlush(device);
        }

        private static void event(RescueEventRepository events, RescueCall call, RescueEventType type,
                UUID actor, String summary, Instant at) {
            events.save(new RescueEvent(call.getId(), call.getEventSequence(), type, actor, summary, at));
        }
    }
}
