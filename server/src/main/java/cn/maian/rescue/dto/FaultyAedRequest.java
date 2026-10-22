package cn.maian.rescue.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FaultyAedRequest(@NotNull UUID expectedDeviceId) {}
