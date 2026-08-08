package cn.maian.rescue.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttachRescueMediaRequest(@NotNull UUID mediaId) {
}
