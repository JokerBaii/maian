CREATE TABLE science_article_interactions (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    article_id VARCHAR(40) NOT NULL,
    liked BOOLEAN NOT NULL DEFAULT FALSE,
    collected BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_science_interaction_user_article UNIQUE (user_id, article_id),
    CONSTRAINT fk_science_interaction_user
        FOREIGN KEY (user_id) REFERENCES user_profiles (id)
);

CREATE INDEX idx_science_interaction_article
    ON science_article_interactions (article_id);
