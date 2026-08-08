package cn.maian.security.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DemoLoginRequest(@NotNull UUID userId) {
}
