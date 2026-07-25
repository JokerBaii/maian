package cn.maian.user.repository;

import cn.maian.user.domain.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {
    List<EmergencyContact> findAllByUserIdOrderByCreatedAtAsc(UUID userId);
}
