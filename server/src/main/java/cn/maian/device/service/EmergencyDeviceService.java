package cn.maian.device.service;

import cn.maian.device.domain.DeviceType;
import cn.maian.device.dto.EmergencyDeviceResponse;
import cn.maian.device.dto.SaveEmergencyDeviceRequest;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.device.repository.EmergencyDeviceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
        return EmergencyDeviceResponse.from(emergencyDeviceRepository.save(device));
    }

    @Transactional
    public EmergencyDeviceResponse update(UUID id, SaveEmergencyDeviceRequest request) {
        validateTypeFields(request);
        var device = findEntity(id);
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
        var device = findEntity(id);
        device.changeStatus(status);
        return EmergencyDeviceResponse.from(device);
    }

    @Transactional
    public void delete(UUID id) {
        emergencyDeviceRepository.delete(findEntity(id));
    }

    private cn.maian.device.domain.EmergencyDevice findEntity(UUID id) {
        return emergencyDeviceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("急救设备不存在"));
    }

    private void validateTypeFields(SaveEmergencyDeviceRequest request) {
        if (request.type() == DeviceType.FIXED && (request.latitude() == 0 || request.longitude() == 0)) {
            throw new IllegalArgumentException("固定设备必须设置经纬度");
        }
        if (request.type() == DeviceType.MOBILE && (request.vehicleInfo() == null || request.vehicleInfo().isBlank())) {
            throw new IllegalArgumentException("移动设备必须填写车辆或携带信息");
        }
    }
}
