ALTER TABLE emergency_devices
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_device_expiry_dispatch
    ON emergency_devices (status, expire_date);

ALTER TABLE rescue_calls
    ADD COLUMN client_request_id VARCHAR(64) NULL,
    ADD UNIQUE INDEX uk_rescue_client_request (client_request_id);
