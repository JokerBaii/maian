package cn.maian.science.repository;

import cn.maian.science.domain.ScienceContent;
import cn.maian.science.domain.ScienceContentKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ScienceContentRepository extends JpaRepository<ScienceContent, String> {
    @Query("""
        select content from ScienceContent content
        where content.active = true and content.kind = cn.maian.science.domain.ScienceContentKind.ARTICLE
          and (:keyword = '' or lower(content.title) like lower(concat('%', :keyword, '%'))
            or lower(content.summary) like lower(concat('%', :keyword, '%')))
        order by content.sortOrder asc, content.publishedAt desc
        """)
    List<ScienceContent> searchPublishedArticles(String keyword, Pageable pageable);
    List<ScienceContent> findAllByActiveTrueAndKindOrderBySortOrderAscPublishedAtDesc(ScienceContentKind kind);
    void deleteBySourceSubmissionId(UUID sourceSubmissionId);
}
