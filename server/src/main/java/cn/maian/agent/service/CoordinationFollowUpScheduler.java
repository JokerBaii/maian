package cn.maian.agent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CoordinationFollowUpScheduler {
    private static final Logger log = LoggerFactory.getLogger(CoordinationFollowUpScheduler.class);
    private final ResourceCoordinationService service;

    public CoordinationFollowUpScheduler(ResourceCoordinationService service) { this.service = service; }

    @Scheduled(initialDelayString = "${app.agent.follow-up-initial-delay-ms:5000}",
        fixedDelayString = "${app.agent.follow-up-interval-ms:5000}")
    public void process() {
        for (var id : service.awaitingFollowUp()) {
            try { service.followUp(id); }
            catch (Exception exception) { log.warn("Could not follow up coordination task {}", id, exception); }
        }
    }
}
