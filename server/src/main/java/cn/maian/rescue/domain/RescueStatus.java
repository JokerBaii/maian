package cn.maian.rescue.domain;

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

    public boolean isActive() {
        return switch (this) {
            case PENDING, MATCHING, EN_ROUTE_TO_AED,
                EN_ROUTE_TO_REQUESTER, ARRIVED, RESCUING, PENDING_CONFIRMATION -> true;
            default -> false;
        };
    }

    public boolean isTerminal() {
        return !isActive();
    }
}
