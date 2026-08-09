package cn.maian.rescue.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record CreateRescueFeedbackRequest(
    @NotNull @Min(1) @Max(5) Integer rating,
    @Size(max = 500) String comment
) {
}
