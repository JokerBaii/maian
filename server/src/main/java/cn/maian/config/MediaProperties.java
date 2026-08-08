package cn.maian.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Validated
@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(
    @NotNull Path directory,
    @Min(1) long maxFileSize,
    @Min(1) long perUserQuotaBytes,
    @Min(1) long capacityBytes,
    @DecimalMin("0.1") @DecimalMax("0.99") double warningRatio,
    @DecimalMin("0.1") @DecimalMax("0.99") double rejectionRatio,
    @Min(1) long downloadTokenMinutes,
    @Min(1) long orphanRetentionHours,
    boolean useXAccel,
    @NotNull String xAccelPrefix
) {
    public MediaProperties {
        if (warningRatio >= rejectionRatio) {
            throw new IllegalArgumentException("媒体存储告警阈值必须小于拒绝阈值");
        }
    }
}
