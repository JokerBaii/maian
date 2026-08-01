package cn.maian.health.service;

import cn.maian.health.domain.WearableDevice;
import cn.maian.health.dto.SaveWearableDeviceRequest;
import cn.maian.health.dto.WearableDeviceResponse;
import cn.maian.health.repository.WearableDeviceRepository;
import cn.maian.user.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WearableDeviceService {

    private final WearableDeviceRepository wearableDeviceRepository;

    public WearableDeviceService(WearableDeviceRepository wearableDeviceRepository) {
        this.wearableDeviceRepository = wearableDeviceRepository;
    }

    @Transactional(readOnly = true)
    public WearableDeviceResponse current() {
        return wearableDeviceRepository.findByUserId(UserProfileService.CURRENT_USER_ID)
            .map(WearableDeviceResponse::from)
            .orElseGet(WearableDeviceResponse::unbound);
    }

    @Transactional
    public WearableDeviceResponse save(SaveWearableDeviceRequest request) {
        var device = wearableDeviceRepository.findByUserId(UserProfileService.CURRENT_USER_ID)
            .orElseGet(() -> WearableDevice.bind(
                UserProfileService.CURRENT_USER_ID,
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
        wearableDeviceRepository.findByUserId(UserProfileService.CURRENT_USER_ID)
            .ifPresent(wearableDeviceRepository::delete);
    }

    @Transactional(readOnly = true)
    public WearableDevice findCurrentEntity() {
        return wearableDeviceRepository.findByUserId(UserProfileService.CURRENT_USER_ID).orElse(null);
    }
}
