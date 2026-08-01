package cn.maian.device.service;

import cn.maian.device.domain.DeviceType;
import cn.maian.device.dto.EmergencyDeviceResponse;
import cn.maian.device.dto.SaveEmergencyDeviceRequest;
import cn.maian.device.dto.UpdateDeviceLocationRequest;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.device.repository.EmergencyDeviceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.Instant;
import cn.maian.user.service.UserProfileService;

@Service
public class EmergencyDeviceService {

    private final EmergencyDeviceRepository emergencyDeviceRepository;

    public EmergencyDeviceService(EmergencyDeviceRepository emergencyDeviceRepository) {
        this.emergencyDeviceRepository = emergencyDeviceRepository;
    }

    @Transactional(readOnly = true)
    public Page<EmergencyDeviceResponse> findAll(DeviceType type, Pageable pageable) {
        var devices = type == null
            ? emergencyDeviceRepository.findAll(pageable)
            : emergencyDeviceRepository.findAllByType(type, pageable);
        return devices.map(EmergencyDeviceResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<EmergencyDeviceResponse> findMine(DeviceType type, Pageable pageable) {
        var devices = type == null
            ? emergencyDeviceRepository.findAllByRegisteredByUserId(
                UserProfileService.CURRENT_USER_ID, pageable
            )
            : emergencyDeviceRepository.findAllByRegisteredByUserIdAndType(
                UserProfileService.CURRENT_USER_ID, type, pageable
            );
        return devices.map(EmergencyDeviceResponse::from);
    }

    @Transactional(readOnly = true)
    public EmergencyDeviceResponse findById(UUID id) {
        return EmergencyDeviceResponse.from(findEntity(id));
    }

    @Transactional
    public EmergencyDeviceResponse create(SaveEmergencyDeviceRequest request) {
        validateTypeFields(request);
        var device = cn.maian.device.domain.EmergencyDevice.create(
            request.type(), request.category(), request.name(), request.address(),
            request.longitude(), request.latitude(), request.ownerPhone(), request.serviceTime(),
            request.expireDate(), request.owner(), request.vehicleInfo(), request.serviceRange(),
            request.instructions(), request.imageUrls(), request.vehicleImageUrls()
        );
        device.registerTo(UserProfileService.CURRENT_USER_ID);
        return EmergencyDeviceResponse.from(emergencyDeviceRepository.save(device));
    }

    @Transactional
    public EmergencyDeviceResponse update(UUID id, SaveEmergencyDeviceRequest request) {
        validateTypeFields(request);
        var device = findEntityForUpdate(id);
        ensureNotReserved(device);
        device.update(
            request.type(), request.category(), request.name(), request.address(),
            request.longitude(), request.latitude(), request.ownerPhone(), request.serviceTime(),
            request.expireDate(), request.owner(), request.vehicleInfo(), request.serviceRange(),
            request.instructions(), request.imageUrls(), request.vehicleImageUrls()
        );
        return EmergencyDeviceResponse.from(device);
    }

    @Transactional
    public EmergencyDeviceResponse updateStatus(UUID id, cn.maian.device.domain.DeviceStatus status) {
        var device = findEntityForUpdate(id);
        if (status == cn.maian.device.domain.DeviceStatus.RESERVED) {
            throw new IllegalArgumentException("设备占用状态只能由救援调度系统设置");
        }
        ensureNotReserved(device);
        device.changeStatus(status);
        return EmergencyDeviceResponse.from(device);
    }

    @Transactional
    public EmergencyDeviceResponse updateLocation(UUID id, UpdateDeviceLocationRequest request) {
        var device = findEntityForUpdate(id);
        if (device.getType() != DeviceType.MOBILE) {
            throw new IllegalArgumentException("只有移动设备可以更新实时位置");
        }
        device.updateLocation(
            request.longitude(),
            request.latitude(),
            request.address(),
            Instant.now()
        );
        return EmergencyDeviceResponse.from(device);
    }

    @Transactional
    public void delete(UUID id) {
        var device = findEntityForUpdate(id);
        ensureNotReserved(device);
        emergencyDeviceRepository.delete(device);
    }

    private cn.maian.device.domain.EmergencyDevice findEntity(UUID id) {
        return emergencyDeviceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("急救设备不存在"));
    }

    private cn.maian.device.domain.EmergencyDevice findEntityForUpdate(UUID id) {
        return emergencyDeviceRepository.findOwnedForUpdateById(
                id,
                UserProfileService.CURRENT_USER_ID
            )
            .orElseThrow(() -> new ResourceNotFoundException("急救设备不存在"));
    }

    private void validateTypeFields(SaveEmergencyDeviceRequest request) {
        if (request.type() == DeviceType.MOBILE && (request.vehicleInfo() == null || request.vehicleInfo().isBlank())) {
            throw new IllegalArgumentException("移动设备必须填写车辆或携带信息");
        }
    }

    private void ensureNotReserved(cn.maian.device.domain.EmergencyDevice device) {
        if (device.getStatus() == cn.maian.device.domain.DeviceStatus.RESERVED) {
            throw new cn.maian.common.exception.InvalidStateTransitionException(
                "设备正在执行救援，暂时不能修改或删除"
            );
        }
    }
}
