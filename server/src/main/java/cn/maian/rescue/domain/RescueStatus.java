package cn.maian.rescue.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum RescueStatus {
    PENDING,
    MATCHING,
    EN_ROUTE_TO_AED,
    EN_ROUTE_TO_REQUESTER,
    ARRIVED,
    RESCUING,
    PENDING_CONFIRMATION,
    COMPLETED,
    NO_RESOURCE,
    EXPIRED,
    USER_CANCELLED,
    SYSTEM_FAILED;

    private static final Set<RescueStatus> ACTIVE_STATUSES = Collections.unmodifiableSet(EnumSet.of(
        PENDING, MATCHING, EN_ROUTE_TO_AED, EN_ROUTE_TO_REQUESTER,
        ARRIVED, RESCUING, PENDING_CONFIRMATION
    ));

    public static Set<RescueStatus> activeStatuses() {
        return ACTIVE_STATUSES;
    }

    public boolean isActive() {
        return ACTIVE_STATUSES.contains(this);
    }

    public boolean isTerminal() {
        return !isActive();
    }
}
