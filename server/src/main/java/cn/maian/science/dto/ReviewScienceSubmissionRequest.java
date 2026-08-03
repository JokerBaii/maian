package cn.maian.science.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewScienceSubmissionRequest(
    @NotNull Boolean approved,
    @Size(max = 300) String reviewNote
) {
}
