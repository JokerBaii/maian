package cn.maian.device.service;

import cn.maian.device.domain.DeviceType;
import cn.maian.device.dto.EmergencyDeviceResponse;
import cn.maian.device.dto.SaveEmergencyDeviceRequest;
import cn.maian.device.dto.UpdateDeviceLocationRequest;
import cn.maian.device.dto.ReviewEmergencyDeviceRequest;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.device.repository.EmergencyDeviceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.Instant;
import cn.maian.user.service.CurrentUserService;

@Service
public class EmergencyDeviceService {

    private final EmergencyDeviceRepository emergencyDeviceRepository;
    private final CurrentUserService currentUserService;

    public EmergencyDeviceService(
        EmergencyDeviceRepository emergencyDeviceRepository,
        CurrentUserService currentUserService
    ) {
        this.emergencyDeviceRepository = emergencyDeviceRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Page<EmergencyDeviceResponse> findAll(DeviceType type, Pageable pageable) {
        var hiddenStatuses = java.util.List.of(
            cn.maian.device.domain.DeviceStatus.PENDING_REVIEW,
            cn.maian.device.domain.DeviceStatus.REJECTED
        );
        var devices = type == null
            ? emergencyDeviceRepository.findAllByStatusNotIn(hiddenStatuses, pageable)
            : emergencyDeviceRepository.findAllByTypeAndStatusNotIn(type, hiddenStatuses, pageable);
        return devices.map(EmergencyDeviceResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<EmergencyDeviceResponse> findPendingReviews(Pageable pageable) {
        currentUserService.requireAnyRole("ADMIN");
        return emergencyDeviceRepository
            .findAllByStatus(cn.maian.device.domain.DeviceStatus.PENDING_REVIEW, pageable)
            .map(EmergencyDeviceResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<EmergencyDeviceResponse> findMine(DeviceType type, Pageable pageable) {
        var devices = type == null
            ? emergencyDeviceRepository.findAllByRegisteredByUserId(
                currentUserService.currentUserId(), pageable
            )
            : emergencyDeviceRepository.findAllByRegisteredByUserIdAndType(
                currentUserService.currentUserId(), type, pageable
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
        device.registerTo(currentUserService.currentUserId());
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
        if (status == cn.maian.device.domain.DeviceStatus.RESERVED
            || status == cn.maian.device.domain.DeviceStatus.PENDING_REVIEW
            || status == cn.maian.device.domain.DeviceStatus.REJECTED) {
            throw new IllegalArgumentException("该设备状态只能由审核或救援流程设置");
        }
        ensureNotReserved(device);
        device.changeStatus(status);
        return EmergencyDeviceResponse.from(device);
    }

    @Transactional
    public EmergencyDeviceResponse review(UUID id, ReviewEmergencyDeviceRequest request) {
        currentUserService.requireAnyRole("ADMIN");
        var device = emergencyDeviceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("急救设备不存在"));
        device.review(request.approved(), request.reviewNote());
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
                currentUserService.currentUserId()
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
