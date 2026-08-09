package cn.maian.user.repository;

import cn.maian.user.domain.UserProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from UserProfile profile where profile.id = :id")
    Optional<UserProfile> findForUpdateById(UUID id);

    Page<UserProfile> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByRole(String role);

    long countByRole(String role);
}
