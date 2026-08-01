package cn.maian.science.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "science_article_interactions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_science_interaction_user_article",
        columnNames = { "user_id", "article_id" }
    )
)
public class ScienceArticleInteraction {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID userId;

    @Column(length = 40, nullable = false)
    private String articleId;

    @Column(nullable = false)
    private boolean liked;

    @Column(nullable = false)
    private boolean collected;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ScienceArticleInteraction() {
    }

    public static ScienceArticleInteraction create(UUID userId, String articleId) {
        var interaction = new ScienceArticleInteraction();
        interaction.id = UUID.randomUUID();
        interaction.userId = userId;
        interaction.articleId = articleId;
        interaction.updatedAt = Instant.now();
        return interaction;
    }

    public void update(boolean liked, boolean collected) {
        this.liked = liked;
        this.collected = collected;
        this.updatedAt = Instant.now();
    }

    public String getArticleId() { return articleId; }
    public boolean isLiked() { return liked; }
    public boolean isCollected() { return collected; }
    public Instant getUpdatedAt() { return updatedAt; }
}
