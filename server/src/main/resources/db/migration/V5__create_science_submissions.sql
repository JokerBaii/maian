CREATE TABLE science_submissions (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    title VARCHAR(50) NOT NULL,
    category VARCHAR(30) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    has_cover_image BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    submitted_at TIMESTAMP(6) NOT NULL,
    INDEX idx_submission_user_time (user_id, submitted_at),
    CONSTRAINT fk_submission_user
        FOREIGN KEY (user_id) REFERENCES user_profiles (id)
        ON DELETE CASCADE
);
