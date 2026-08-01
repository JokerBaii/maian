package cn.maian.science.repository;

import cn.maian.science.domain.ScienceSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ScienceSubmissionRepository extends JpaRepository<ScienceSubmission, UUID> {
    long countByUserId(UUID userId);
    Page<ScienceSubmission> findAllByUserId(UUID userId, Pageable pageable);
}
