CREATE TABLE rescue_call_images (
    rescue_call_id CHAR(36) NOT NULL,
    position INT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    PRIMARY KEY (rescue_call_id, position),
    CONSTRAINT fk_rescue_images_call
        FOREIGN KEY (rescue_call_id) REFERENCES rescue_calls (id)
        ON DELETE CASCADE
);
