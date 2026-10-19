package cn.maian.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class CoordinationSchedulingConfiguration {
    // Explicit default scheduler keeps rescue rematching independent of model latency.
    @Bean(name = "taskScheduler")
    ThreadPoolTaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("maian-workflow-");
        return scheduler;
    }
    @Bean(name = "coordinationScheduler")
    ThreadPoolTaskScheduler coordinationScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("maian-coordination-");
        return scheduler;
    }
}
