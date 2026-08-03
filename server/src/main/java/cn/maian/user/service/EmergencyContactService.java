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
    private final CurrentUserService currentUserService;

    public EmergencyContactService(
        EmergencyContactRepository emergencyContactRepository,
        CurrentUserService currentUserService
    ) {
        this.emergencyContactRepository = emergencyContactRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> findAll() {
        return emergencyContactRepository
            .findAllByUserIdOrderByCreatedAtAsc(currentUserService.currentUserId())
            .stream()
            .map(EmergencyContactResponse::from)
            .toList();
    }

    @Transactional
    public EmergencyContactResponse create(SaveEmergencyContactRequest request) {
        var contact = EmergencyContact.create(
            currentUserService.currentUserId(),
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
        if (!currentUserService.currentUserId().equals(contact.getUserId())) {
            throw new ResourceNotFoundException("紧急联系人不存在");
        }
        return contact;
    }
}
