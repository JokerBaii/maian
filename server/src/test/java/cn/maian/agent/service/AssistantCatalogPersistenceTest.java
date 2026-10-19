package cn.maian.agent.service;

import cn.maian.science.domain.ScienceContent;
import cn.maian.science.domain.ScienceSubmission;
import cn.maian.science.repository.ScienceContentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class AssistantCatalogPersistenceTest {
    @Autowired ScienceContentRepository contents;
    @Autowired EntityManager entityManager;
    @Test
    void recommendationsExcludeInactiveArticlesAndMatchRealTopic() {
        var visible = contents.save(article("AED 使用学习", "设备学习资料"));
        var hidden = contents.save(article("AED 下架内容", "不应被推荐"));
        contents.save(article("运动与健康", "日常运动知识"));
        entityManager.flush();
        entityManager.createQuery("update ScienceContent content set content.active = false where content.id = :id")
            .setParameter("id", hidden.getId()).executeUpdate();
        entityManager.clear();
        assertThat(contents.searchPublishedArticles("aed", PageRequest.of(0, 3)))
            .extracting(ScienceContent::getId).containsExactly(visible.getId());
    }
    private ScienceContent article(String title, String text) {
        var submission = ScienceSubmission.create(UUID.randomUUID(), title, "device", text, null);
        submission.review(true, "已审核");
        return ScienceContent.fromApprovedSubmission(submission);
    }
}
