package cn.maian.device.dto;

import cn.maian.device.domain.DeviceType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record SaveEmergencyDeviceRequest(
    @NotNull DeviceType type,
    @NotBlank @Size(max = 30) String category,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 255) String address,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
    @Size(max = 40) String ownerPhone,
    @Size(max = 80) String serviceTime,
    LocalDate expireDate,
    @Size(max = 120) String owner,
    @Size(max = 120) String vehicleInfo,
    @Min(1) @Max(100) Integer serviceRange,
    @Size(max = 500) String instructions,
    @Size(max = 9) List<@NotBlank @Size(max = 500) String> imageUrls,
    @Size(max = 9) List<@NotBlank @Size(max = 500) String> vehicleImageUrls
) {
    public SaveEmergencyDeviceRequest {
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
        vehicleImageUrls = vehicleImageUrls == null ? List.of() : List.copyOf(vehicleImageUrls);
    }
}
