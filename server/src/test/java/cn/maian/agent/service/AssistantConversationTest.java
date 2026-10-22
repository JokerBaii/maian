package cn.maian.agent.service;

import cn.maian.agent.dto.AssistantContext;
import cn.maian.science.repository.ScienceContentRepository;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssistantConversationTest {
    private final AssistantService service=new AssistantService(mock(ScienceContentRepository.class),mock(AgentModelGateway.class));
    private AssistantContext.Pending pending(String op,List<String> keys,List<String> missing){return new AssistantContext.Pending(op,keys,missing);}
    @Test void extractsContactFieldsWithoutRecreatingTheTaskOrRepeatingSensitiveValues(){
        var p=pending("CONTACT_SAVE",List.of("name","phone","relation"),List.of("name","phone","relation"));
        var first=service.assist("我妈妈",true,null,p);
        assertThat(first.continuation()).isTrue();assertThat(first.tasks().getFirst().parameters()).containsEntry("name","妈妈").containsEntry("relation","妈妈");
        var second=service.assist("手机号13800000000",true,null,pending("CONTACT_SAVE",List.of("name","phone","relation"),List.of("phone")));
        assertThat(second.continuation()).isTrue();assertThat(second.tasks().getFirst().parameters()).containsExactlyEntriesOf(Map.of("phone","13800000000"));
    }
    @Test void relationDoesNotOverwriteAMissingNameAndNewCommandsRemainIndependent(){
        var p=pending("CONTACT_SAVE",List.of("name","phone","relation"),List.of("name","phone","relation"));
        assertThat(AssistantContinuation.local("关系是母亲",p)).containsExactlyEntriesOf(Map.of("relation","母亲"));
        assertThat(service.assist("查看我的设备",false,null,p).continuation()).isFalse();
        assertThat(service.assist("查看我的设备",false,null,p).tasks().getFirst().operation()).isEqualTo("DEVICE_LIST");
        assertThat(AssistantContinuation.local("不要保存",p)).isEmpty();
        assertThat(AssistantContinuation.local("标题改成急救分享",pending("SCIENCE_SUBMIT",List.of("title"),List.of("title")))).containsEntry("title","急救分享");
    }
    @Test void usesCurrentPageForReferencesButDoesNotInferNegatedPhysicalActions(){
        var c=new AssistantContext("pages/rescue/detail",null,UUID.randomUUID().toString(),null,null,null,null,null,null,null);
        assertThat(AssistantOperations.local("我已到达现场",c).getFirst().operation()).isEqualTo("RESCUE_ARRIVAL");
        assertThat(AssistantOperations.local("确认救援完成",c).getFirst().operation()).isEqualTo("RESCUE_CONFIRM_COMPLETION");
        assertThat(AssistantOperations.local("我还没到达现场",c)).isEmpty();
        assertThat(AssistantOperations.local("如果到了现场",c)).isEmpty();
        assertThat(AssistantContinuation.editable(pending("EXECUTE_URL",List.of("url"),List.of("url")))).isEmpty();
    }
    @Test void parsesExplicitReminderTimesInShanghaiWithoutGuessingMissingTimes(){
        var now=Instant.parse("2026-10-20T13:00:00Z");
        assertThat(AssistantReminderLanguage.time("十分钟后提醒我归还AED",now)).isEqualTo(now.plusSeconds(600));
        assertThat(AssistantReminderLanguage.time("明天上午8点提醒我",now)).isEqualTo(Instant.parse("2026-10-21T00:00:00Z"));
        assertThat(AssistantReminderLanguage.time("明天下午三点提醒我",now)).isEqualTo(Instant.parse("2026-10-21T07:00:00Z"));
        assertThat(AssistantReminderLanguage.time("明早八点半提醒我",now)).isEqualTo(Instant.parse("2026-10-21T00:30:00Z"));
        assertThat(AssistantReminderLanguage.time("2026-10-25 08:30提醒我",now)).isEqualTo(Instant.parse("2026-10-25T00:30:00Z"));
        assertThat(AssistantReminderLanguage.time("明天提醒我",now)).isNull();
        assertThat(AssistantReminderLanguage.time("今天上午8点",now)).isNull();
    }
}
