package cn.maian.device.dto;

import cn.maian.device.domain.DeviceType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SaveEmergencyDeviceRequest(
    @NotNull DeviceType type,
    @NotBlank @Size(max = 30) String category,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 255) String address,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
    @Size(max = 40) String ownerPhone,
    @NotNull @Size(max = 14) List<@Valid DeviceServiceWindowRequest> serviceWindows,
    LocalDate expireDate,
    @Size(max = 120) String owner,
    @Size(max = 120) String vehicleInfo,
    @Min(1) @Max(100) Integer serviceRange,
    @Size(max = 500) String instructions,
    @Size(max = 9) List<@NotNull UUID> imageMediaIds,
    @Size(max = 9) List<@NotNull UUID> vehicleImageMediaIds
) {
    public SaveEmergencyDeviceRequest {
        serviceWindows = serviceWindows == null ? List.of() : List.copyOf(serviceWindows);
        imageMediaIds = imageMediaIds == null ? List.of() : List.copyOf(imageMediaIds);
        vehicleImageMediaIds = vehicleImageMediaIds == null ? List.of() : List.copyOf(vehicleImageMediaIds);
    }
}
