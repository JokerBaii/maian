ALTER TABLE emergency_devices
    ADD COLUMN last_location_at TIMESTAMP(6) NULL,
    ADD COLUMN reserved_for_call_id CHAR(36) NULL,
    ADD COLUMN reserved_at TIMESTAMP(6) NULL,
    ADD UNIQUE INDEX uk_device_active_rescue (reserved_for_call_id),
    ADD INDEX idx_device_dispatch (category, status, latitude, longitude),
    ADD INDEX idx_mobile_location_freshness (type, status, last_location_at);

UPDATE emergency_devices
SET last_location_at = CURRENT_TIMESTAMP(6)
WHERE type = 'MOBILE';

ALTER TABLE rescue_calls
    ADD COLUMN matched_device_id CHAR(36) NULL,
    ADD COLUMN matched_at TIMESTAMP(6) NULL,
    ADD COLUMN matched_distance_meters INT NULL,
    ADD COLUMN estimated_arrival_seconds INT NULL,
    ADD COLUMN match_strategy VARCHAR(40) NULL,
    ADD INDEX idx_rescue_matched_device (matched_device_id),
    ADD CONSTRAINT fk_rescue_matched_device
        FOREIGN KEY (matched_device_id) REFERENCES emergency_devices (id)
        ON DELETE SET NULL;
