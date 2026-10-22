package cn.maian.agent.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AssistantReminderScheduler {
    private static final Logger LOG=LoggerFactory.getLogger(AssistantReminderScheduler.class);
    private final AssistantReminderService service;
    public AssistantReminderScheduler(AssistantReminderService service){this.service=service;}
    @Scheduled(scheduler="reminderScheduler",fixedDelayString="${app.agent.reminder-interval-ms:10000}",initialDelayString="${app.agent.reminder-initial-delay-ms:10000}")
    public void process(){for(var id:service.pending())try{service.check(id);}catch(Exception error){LOG.warn("Reminder check failed: {}",id,error);}}
}
