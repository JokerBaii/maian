package cn.maian.health.service;

import cn.maian.health.domain.WearableDevice;
import cn.maian.health.dto.SaveWearableDeviceRequest;
import cn.maian.health.dto.WearableDeviceResponse;
import cn.maian.health.repository.WearableDeviceRepository;
import cn.maian.user.service.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WearableDeviceService {

    private final WearableDeviceRepository wearableDeviceRepository;
    private final CurrentUserService currentUserService;

    public WearableDeviceService(
        WearableDeviceRepository wearableDeviceRepository,
        CurrentUserService currentUserService
    ) {
        this.wearableDeviceRepository = wearableDeviceRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public WearableDeviceResponse current() {
        return wearableDeviceRepository.findByUserId(currentUserService.currentUserId())
            .map(WearableDeviceResponse::from)
            .orElseGet(WearableDeviceResponse::unbound);
    }

    @Transactional
    public WearableDeviceResponse save(SaveWearableDeviceRequest request) {
        var device = wearableDeviceRepository.findByUserId(currentUserService.currentUserId())
            .orElseGet(() -> WearableDevice.bind(
                currentUserService.currentUserId(),
                request.deviceIdentifier(),
                request.name(),
                request.type(),
                request.connected(),
                request.battery()
            ));
        device.update(
            request.deviceIdentifier(),
            request.name(),
            request.type(),
            request.connected(),
            request.battery()
        );
        return WearableDeviceResponse.from(wearableDeviceRepository.save(device));
    }

    @Transactional
    public void delete() {
        wearableDeviceRepository.findByUserId(currentUserService.currentUserId())
            .ifPresent(wearableDeviceRepository::delete);
    }

    @Transactional(readOnly = true)
    public WearableDevice findCurrentEntity() {
        return wearableDeviceRepository.findByUserId(currentUserService.currentUserId()).orElse(null);
    }
}
