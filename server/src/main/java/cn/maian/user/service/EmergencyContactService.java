package cn.maian.user.service;

import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.user.domain.EmergencyContact;
import cn.maian.user.dto.EmergencyContactResponse;
import cn.maian.user.dto.SaveEmergencyContactRequest;
import cn.maian.user.repository.EmergencyContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EmergencyContactService {

    private final EmergencyContactRepository emergencyContactRepository;

    public EmergencyContactService(EmergencyContactRepository emergencyContactRepository) {
        this.emergencyContactRepository = emergencyContactRepository;
    }

    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> findAll() {
        return emergencyContactRepository
            .findAllByUserIdOrderByCreatedAtAsc(UserProfileService.CURRENT_USER_ID)
            .stream()
            .map(EmergencyContactResponse::from)
            .toList();
    }

    @Transactional
    public EmergencyContactResponse create(SaveEmergencyContactRequest request) {
        var contact = EmergencyContact.create(
            UserProfileService.CURRENT_USER_ID,
            request.name().trim(),
            request.phone().trim(),
            request.relation().trim()
        );
        return EmergencyContactResponse.from(emergencyContactRepository.save(contact));
    }

    @Transactional
    public EmergencyContactResponse update(UUID id, SaveEmergencyContactRequest request) {
        var contact = findOwned(id);
        contact.update(request.name().trim(), request.phone().trim(), request.relation().trim());
        return EmergencyContactResponse.from(contact);
    }

    @Transactional
    public void delete(UUID id) {
        emergencyContactRepository.delete(findOwned(id));
    }

    private EmergencyContact findOwned(UUID id) {
        var contact = emergencyContactRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("紧急联系人不存在"));
        if (!UserProfileService.CURRENT_USER_ID.equals(contact.getUserId())) {
            throw new ResourceNotFoundException("紧急联系人不存在");
        }
        return contact;
    }
}
