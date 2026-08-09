package cn.maian.science.repository;

import cn.maian.science.domain.ScienceContent;
import cn.maian.science.domain.ScienceContentKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScienceContentRepository extends JpaRepository<ScienceContent, String> {
    List<ScienceContent> findAllByActiveTrueAndKindOrderBySortOrderAscPublishedAtDesc(ScienceContentKind kind);
    void deleteBySourceSubmissionId(UUID sourceSubmissionId);
}
