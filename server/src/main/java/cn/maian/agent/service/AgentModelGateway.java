package cn.maian.agent.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AgentModelGateway {
    private static final Logger log = LoggerFactory.getLogger(AgentModelGateway.class);
    private final boolean enabled;
    private final ObjectProvider<ChatClient.Builder> builders;
    private final ExecutorService executor = new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(4), Thread.ofPlatform().daemon().name("maian-agent-model-", 0).factory(),
        new ThreadPoolExecutor.AbortPolicy());

    public AgentModelGateway(@Value("${app.ai.enabled:false}") boolean enabled,
        ObjectProvider<ChatClient.Builder> builders) {
        this.enabled = enabled;
        this.builders = builders;
    }

    public String invoke(String instruction, BoundedTools tools) {
        return invoke(instruction, "请使用提供的工具完成当前任务。", tools);
    }

    public String invoke(String instruction, String input, BoundedTools tools) {
        if (!enabled) return "RULES";
        Future<?> future = null;
        try {
            future = executor.submit(() -> builders.getObject().build().prompt()
                .system(instruction).user(input)
                .tools(tools).call().content());
            future.get(12, TimeUnit.SECONDS);
            return "SPRING_AI";
        } catch (Exception exception) {
            if (future != null) future.cancel(true);
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Agent model unavailable; using bounded business workflow ({})", exception.getClass().getSimpleName());
            return "RULE_FALLBACK";
        } finally {
            tools.close();
        }
    }

    public abstract static class BoundedTools {
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicInteger calls = new AtomicInteger();
        protected void checkBudget() {
            if (closed.get() || calls.incrementAndGet() > 8 || Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("工具执行窗口已结束");
            }
        }
        public void close() { closed.set(true); }
    }

    @PreDestroy
    void shutdown() { executor.shutdownNow(); }
}
