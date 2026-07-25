package cn.maian.science.repository;

import cn.maian.science.domain.ScienceSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScienceSubmissionRepository extends JpaRepository<ScienceSubmission, UUID> {
    long countByUserId(UUID userId);
}
