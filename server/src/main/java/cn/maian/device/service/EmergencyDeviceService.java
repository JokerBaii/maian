package cn.maian.device.service;

import cn.maian.device.domain.DeviceType;
import cn.maian.device.domain.DeviceServiceWindow;
import cn.maian.device.dto.PublicEmergencyDeviceResponse;
import cn.maian.device.dto.OwnerEmergencyDeviceResponse;
import cn.maian.device.dto.AdminEmergencyDeviceResponse;
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
import cn.maian.media.service.MediaStorageService;
import cn.maian.media.domain.MediaPurpose;
import cn.maian.config.DispatchProperties;
import java.time.Clock;

@Service
public class EmergencyDeviceService {

    private final EmergencyDeviceRepository emergencyDeviceRepository;
    private final CurrentUserService currentUserService;
    private final MediaStorageService mediaStorageService;
    private final DispatchProperties dispatchProperties;
    private final Clock clock;

    public EmergencyDeviceService(
        EmergencyDeviceRepository emergencyDeviceRepository,
        CurrentUserService currentUserService,
        MediaStorageService mediaStorageService,
        DispatchProperties dispatchProperties,
        Clock clock
    ) {
        this.emergencyDeviceRepository = emergencyDeviceRepository;
        this.currentUserService = currentUserService;
        this.mediaStorageService = mediaStorageService;
        this.dispatchProperties = dispatchProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<PublicEmergencyDeviceResponse> findAll(DeviceType type, Pageable pageable) {
        var publicStatuses = java.util.List.of(
            cn.maian.device.domain.DeviceStatus.AVAILABLE,
            cn.maian.device.domain.DeviceStatus.RESERVED
        );
        var devices = type == null
            ? emergencyDeviceRepository.findAllByStatusIn(publicStatuses, pageable)
            : emergencyDeviceRepository.findAllByTypeAndStatusIn(type, publicStatuses, pageable);
        return devices.map(this::toPublicResponse);
    }

    @Transactional(readOnly = true)
    public Page<AdminEmergencyDeviceResponse> findPendingReviews(Pageable pageable) {
        currentUserService.requireAnyRole("ADMIN");
        return emergencyDeviceRepository
            .findAllByStatus(cn.maian.device.domain.DeviceStatus.PENDING_REVIEW, pageable)
            .map(this::toAdminResponse);
    }

    @Transactional(readOnly = true)
    public Page<OwnerEmergencyDeviceResponse> findMine(DeviceType type, Pageable pageable) {
        var devices = type == null
            ? emergencyDeviceRepository.findAllByRegisteredByUserId(
                currentUserService.currentUserId(), pageable
            )
            : emergencyDeviceRepository.findAllByRegisteredByUserIdAndType(
                currentUserService.currentUserId(), type, pageable
            );
        return devices.map(this::toOwnerResponse);
    }

    @Transactional(readOnly = true)
    public PublicEmergencyDeviceResponse findPublicById(UUID id) {
        var device = findEntity(id);
        if (device.getStatus() != cn.maian.device.domain.DeviceStatus.AVAILABLE
            && device.getStatus() != cn.maian.device.domain.DeviceStatus.RESERVED) {
            throw new ResourceNotFoundException("急救设备不存在");
        }
        return toPublicResponse(device);
    }

    @Transactional(readOnly = true)
    public OwnerEmergencyDeviceResponse findMineById(UUID id) {
        var device = emergencyDeviceRepository.findOwnedById(id, currentUserService.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("急救设备不存在"));
        return toOwnerResponse(device);
    }

    @Transactional
    public OwnerEmergencyDeviceResponse create(SaveEmergencyDeviceRequest request) {
        validateTypeFields(request);
        var device = cn.maian.device.domain.EmergencyDevice.create(
            request.type(), request.category(), request.name(), request.address(),
            request.longitude(), request.latitude(), request.ownerPhone(), toServiceWindows(request),
            request.expireDate(), request.owner(), request.vehicleInfo(), request.serviceRange(),
            request.instructions(), request.imageMediaIds(), request.vehicleImageMediaIds()
        );
        device.registerTo(currentUserService.currentUserId());
        device = emergencyDeviceRepository.save(device);
        syncMedia(device);
        return toOwnerResponse(device);
    }

    @Transactional
    public OwnerEmergencyDeviceResponse update(UUID id, SaveEmergencyDeviceRequest request) {
        validateTypeFields(request);
        var device = findEntityForUpdate(id);
        ensureNotReserved(device);
        device.update(
            request.type(), request.category(), request.name(), request.address(),
            request.longitude(), request.latitude(), request.ownerPhone(), toServiceWindows(request),
            request.expireDate(), request.owner(), request.vehicleInfo(), request.serviceRange(),
            request.instructions(), request.imageMediaIds(), request.vehicleImageMediaIds()
        );
        syncMedia(device);
        return toOwnerResponse(device);
    }

    @Transactional
    public OwnerEmergencyDeviceResponse enable(UUID id) {
        var device = findEntityForUpdate(id);
        device.enable();
        return toOwnerResponse(device);
    }

    @Transactional
    public OwnerEmergencyDeviceResponse disable(UUID id) {
        var device = findEntityForUpdate(id);
        device.disable();
        return toOwnerResponse(device);
    }

    @Transactional
    public AdminEmergencyDeviceResponse review(UUID id, ReviewEmergencyDeviceRequest request) {
        currentUserService.requireAnyRole("ADMIN");
        var device = emergencyDeviceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("急救设备不存在"));
        device.review(request.approved(), request.reviewNote());
        mediaStorageService.setReferencePublic("EMERGENCY_DEVICE", id, request.approved());
        return toAdminResponse(device);
    }

    @Transactional
    public OwnerEmergencyDeviceResponse updateLocation(UUID id, UpdateDeviceLocationRequest request) {
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
        return toOwnerResponse(device);
    }

    @Transactional
    public void delete(UUID id) {
        var device = findEntityForUpdate(id);
        ensureNotReserved(device);
        emergencyDeviceRepository.delete(device);
        mediaStorageService.detachReference("EMERGENCY_DEVICE", id);
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
        if (request.serviceWindows().isEmpty()) {
            throw new IllegalArgumentException("至少需要设置一个服务时段");
        }
        if (request.type() == DeviceType.MOBILE && (request.vehicleInfo() == null || request.vehicleInfo().isBlank())) {
            throw new IllegalArgumentException("移动设备必须填写车辆或携带信息");
        }
    }

    private java.util.List<DeviceServiceWindow> toServiceWindows(SaveEmergencyDeviceRequest request) {
        return request.serviceWindows().stream()
            .map(window -> new DeviceServiceWindow(
                window.dayOfWeek(), window.opensAt(), window.closesAt()
            ))
            .toList();
    }

    private void ensureNotReserved(cn.maian.device.domain.EmergencyDevice device) {
        if (device.getStatus() == cn.maian.device.domain.DeviceStatus.RESERVED) {
            throw new cn.maian.common.exception.InvalidStateTransitionException(
                "设备正在执行救援，暂时不能修改或删除"
            );
        }
    }

    private void syncMedia(cn.maian.device.domain.EmergencyDevice device) {
        boolean makePublic = device.getStatus() == cn.maian.device.domain.DeviceStatus.AVAILABLE;
        mediaStorageService.syncOwnedReference(
            device.getImageMediaIds(), MediaPurpose.DEVICE_IMAGE,
            "EMERGENCY_DEVICE", device.getId(), makePublic
        );
        mediaStorageService.syncOwnedReference(
            device.getVehicleImageMediaIds(), MediaPurpose.VEHICLE_IMAGE,
            "EMERGENCY_DEVICE", device.getId(), makePublic
        );
    }

    private PublicEmergencyDeviceResponse toPublicResponse(
        cn.maian.device.domain.EmergencyDevice device
    ) {
        return PublicEmergencyDeviceResponse.from(
            device, clock.instant(), dispatchProperties.mobileLocationMaxAgeSeconds()
        );
    }

    private OwnerEmergencyDeviceResponse toOwnerResponse(
        cn.maian.device.domain.EmergencyDevice device
    ) {
        return OwnerEmergencyDeviceResponse.from(
            device, clock.instant(), dispatchProperties.mobileLocationMaxAgeSeconds()
        );
    }

    private AdminEmergencyDeviceResponse toAdminResponse(
        cn.maian.device.domain.EmergencyDevice device
    ) {
        return AdminEmergencyDeviceResponse.from(
            device, clock.instant(), dispatchProperties.mobileLocationMaxAgeSeconds()
        );
    }
}
