ALTER TABLE emergency_devices
    ADD COLUMN registered_by_user_id CHAR(36) NULL,
    ADD INDEX idx_device_registered_user (registered_by_user_id, type, created_at);

UPDATE emergency_devices
SET registered_by_user_id = '30000000-0000-0000-0000-000000000001'
WHERE registered_by_user_id IS NULL;

ALTER TABLE emergency_devices
    MODIFY registered_by_user_id CHAR(36) NOT NULL,
    ADD CONSTRAINT fk_device_registered_user
        FOREIGN KEY (registered_by_user_id) REFERENCES user_profiles (id)
        ON DELETE RESTRICT;

ALTER TABLE rescue_calls
    ADD COLUMN requested_by_user_id CHAR(36) NULL,
    ADD INDEX idx_rescue_requested_user (requested_by_user_id, created_at);

UPDATE rescue_calls
SET requested_by_user_id = '30000000-0000-0000-0000-000000000001'
WHERE requested_by_user_id IS NULL;

ALTER TABLE rescue_calls
    MODIFY requested_by_user_id CHAR(36) NOT NULL,
    ADD CONSTRAINT fk_rescue_requested_user
        FOREIGN KEY (requested_by_user_id) REFERENCES user_profiles (id)
        ON DELETE RESTRICT;
