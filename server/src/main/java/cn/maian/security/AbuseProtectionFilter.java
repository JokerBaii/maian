package cn.maian.security;

import cn.maian.common.api.ApiResponse;
import cn.maian.config.AbuseProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AbuseProtectionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AbuseProtectionFilter.class);

    private final AbuseProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public AbuseProtectionFilter(AbuseProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Limit limit = resolveLimit(request);
        if (limit != null && (!allow(limit.primaryKey(), limit.primaryLimit())
            || (limit.secondaryKey() != null && !allow(limit.secondaryKey(), limit.secondaryLimit())))) {
            log.warn("Abuse limit exceeded for {} {} from {}", request.getMethod(), request.getRequestURI(), clientIp(request));
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.error("RATE_LIMITED", "请求过于频繁，请稍后重试")
            );
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Limit resolveLimit(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String user = currentSubject();
        if ("POST".equals(method) && "/api/v1/rescue-calls".equals(path)) {
            return new Limit(
                "rescue:user:" + user, properties.rescueUserLimit(),
                "rescue:ip:" + clientIp(request), properties.rescueIpLimit()
            );
        }
        if ("POST".equals(method) && "/api/v1/media".equals(path)) {
            return new Limit("media:user:" + user, properties.mediaUserLimit(), null, 0);
        }
        if ("PATCH".equals(method) && path.matches("/api/v1/emergency-devices/[0-9a-fA-F-]{36}/location")) {
            return new Limit("heartbeat:user:" + user, properties.mobileHeartbeatUserLimit(), null, 0);
        }
        return null;
    }

    private boolean allow(String key, int limit) {
        long window = clock.instant().getEpochSecond() / properties.windowSeconds();
        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.window() != window) {
                return new WindowCounter(window, 1);
            }
            return new WindowCounter(window, existing.count() + 1);
        });
        if (counters.size() > 10_000) {
            counters.entrySet().removeIf(entry -> entry.getValue().window() < window - 1);
        }
        return counter.count() <= limit;
    }

    private String currentSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "anonymous" : authentication.getName();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record WindowCounter(long window, int count) {
    }

    private record Limit(String primaryKey, int primaryLimit, String secondaryKey, int secondaryLimit) {
    }
}
