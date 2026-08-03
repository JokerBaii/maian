package cn.maian.user.context;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

@Component
@RequestScope
public class DemoUserContext {

    public static final UUID DEFAULT_USER_ID =
        UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final String USER_HEADER = "X-Demo-User-Id";

    private final UUID userId;

    public DemoUserContext(HttpServletRequest request) {
        String value = request.getHeader(USER_HEADER);
        this.userId = parseUserId(value);
    }

    public UUID userId() {
        return userId;
    }

    private UUID parseUserId(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_USER_ID;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("用户标识无效");
        }
    }
}
