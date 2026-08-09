ALTER TABLE science_article_interactions
  MODIFY COLUMN article_id VARCHAR(80) NOT NULL;

ALTER TABLE user_profiles
  ADD COLUMN avatar_url VARCHAR(255) NULL AFTER role;
