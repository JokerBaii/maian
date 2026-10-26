package cn.maian.agent.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** References select a context, never grant permission to a domain object. */
public record AssistantContext(
    @Size(max = 100) @Pattern(regexp = "^pages/[a-zA-Z0-9/_-]+$") String page,
    @Pattern(regexp = "^[a-fA-F0-9-]{36}$") String deviceId,
    @Pattern(regexp = "^[a-fA-F0-9-]{36}$") String rescueId,
    @Pattern(regexp = "^[a-fA-F0-9-]{36}$") String reportId,
    @Size(max = 100) @Pattern(regexp = "^[a-zA-Z0-9_-]+$") String articleId,
    @Pattern(regexp = "^[a-fA-F0-9-]{36}$") String contactId,
    @Pattern(regexp = "^[a-fA-F0-9-]{36}$") String submissionId,
    @Pattern(regexp = "^[a-fA-F0-9-]{36}$") String reminderId,
    @Pattern(regexp = "^[a-fA-F0-9-]{36}$") String accountId,
    @Size(max = 40) @Pattern(regexp = "^[a-zA-Z]+$") String settingKey,
    @Pattern(regexp = "^[a-fA-F0-9-]{36}$") String documentId
) {
    public record Turn(@jakarta.validation.constraints.NotBlank @Size(max = 300) String question,
        @jakarta.validation.constraints.NotBlank @Size(max = 1000) String reply) {}
    public record Pending(@jakarta.validation.constraints.NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Z_]+$") String operation,
        @Size(max = 15) List<@Pattern(regexp = "^[a-zA-Z]+$") @Size(max = 40) String> editableKeys,
        @Size(max = 15) List<@Pattern(regexp = "^[a-zA-Z]+$") @Size(max = 40) String> missingKeys) {}
}
