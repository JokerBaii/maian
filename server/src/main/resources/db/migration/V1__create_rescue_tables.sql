CREATE TABLE rescue_calls (
    id CHAR(36) PRIMARY KEY,
    urgency VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    address VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX idx_rescue_status_created (status, created_at),
    INDEX idx_rescue_location (latitude, longitude)
);

CREATE TABLE rescue_call_symptoms (
    rescue_call_id CHAR(36) NOT NULL,
    symptom VARCHAR(50) NOT NULL,
    PRIMARY KEY (rescue_call_id, symptom),
    CONSTRAINT fk_rescue_symptoms_call
        FOREIGN KEY (rescue_call_id) REFERENCES rescue_calls (id)
        ON DELETE CASCADE
);
