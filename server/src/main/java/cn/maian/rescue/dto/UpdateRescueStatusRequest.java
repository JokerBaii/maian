package cn.maian.rescue.dto;

import cn.maian.rescue.domain.RescueStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateRescueStatusRequest(
    @NotNull RescueStatus status
) {
}
