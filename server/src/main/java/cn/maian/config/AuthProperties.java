package cn.maian.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
    @NotBlank String issuer,
    @NotBlank @Size(min = 32) String secret,
    @Min(1) long accessTokenMinutes
) {
}
