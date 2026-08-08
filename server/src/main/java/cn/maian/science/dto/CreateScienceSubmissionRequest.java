package cn.maian.science.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateScienceSubmissionRequest(
    @NotBlank @Size(max = 50) String title,
    @NotBlank
    @Pattern(regexp = "^(device|emergency|health|exercise)$")
    String category,
    @NotBlank @Size(max = 2000) String content,
    UUID coverMediaId
) {
}
