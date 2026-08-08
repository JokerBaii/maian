package cn.maian.security;

import java.util.Set;
import java.util.UUID;

public final class DemoAccounts {

    public static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    public static final UUID VOLUNTEER_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    public static final UUID ADMIN_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    public static final Set<UUID> IDS = Set.of(USER_ID, VOLUNTEER_ID, ADMIN_ID);

    private DemoAccounts() {
    }
}
