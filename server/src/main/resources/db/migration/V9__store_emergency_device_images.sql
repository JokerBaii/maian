CREATE TABLE emergency_device_images (
    device_id CHAR(36) NOT NULL,
    position INT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    PRIMARY KEY (device_id, position),
    CONSTRAINT fk_device_images_device
        FOREIGN KEY (device_id) REFERENCES emergency_devices (id)
        ON DELETE CASCADE
);

CREATE TABLE emergency_device_vehicle_images (
    device_id CHAR(36) NOT NULL,
    position INT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    PRIMARY KEY (device_id, position),
    CONSTRAINT fk_device_vehicle_images_device
        FOREIGN KEY (device_id) REFERENCES emergency_devices (id)
        ON DELETE CASCADE
);
