CREATE TABLE user_settings (
    user_id CHAR(36) PRIMARY KEY,
    rescue_push BOOLEAN NOT NULL DEFAULT TRUE,
    health_alert BOOLEAN NOT NULL DEFAULT TRUE,
    science_update BOOLEAN NOT NULL DEFAULT FALSE,
    location_share BOOLEAN NOT NULL DEFAULT TRUE,
    health_data_share BOOLEAN NOT NULL DEFAULT FALSE,
    max_heart_rate INT NOT NULL DEFAULT 120,
    min_heart_rate INT NOT NULL DEFAULT 50,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_settings_user
        FOREIGN KEY (user_id) REFERENCES user_profiles (id)
        ON DELETE CASCADE
);

CREATE TABLE wearable_devices (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    device_identifier VARCHAR(160) NOT NULL,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(40) NOT NULL,
    connected BOOLEAN NOT NULL DEFAULT FALSE,
    battery INT NULL,
    last_seen_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE INDEX uk_wearable_user (user_id),
    UNIQUE INDEX uk_wearable_identifier (device_identifier),
    CONSTRAINT fk_wearable_user
        FOREIGN KEY (user_id) REFERENCES user_profiles (id)
        ON DELETE CASCADE
);

CREATE TABLE heart_rate_readings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    wearable_id CHAR(36) NULL,
    bpm INT NOT NULL,
    scene VARCHAR(20) NOT NULL,
    recorded_at TIMESTAMP(6) NOT NULL,
    INDEX idx_heart_rate_user_time (user_id, recorded_at DESC),
    CONSTRAINT fk_heart_rate_user
        FOREIGN KEY (user_id) REFERENCES user_profiles (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_heart_rate_wearable
        FOREIGN KEY (wearable_id) REFERENCES wearable_devices (id)
        ON DELETE SET NULL
);

INSERT INTO user_settings (
    user_id, rescue_push, health_alert, science_update,
    location_share, health_data_share, max_heart_rate, min_heart_rate, updated_at
) VALUES (
    '30000000-0000-0000-0000-000000000001',
    TRUE, TRUE, FALSE, TRUE, FALSE, 120, 50, UTC_TIMESTAMP(6)
);
