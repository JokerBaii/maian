DROP TABLE IF EXISTS emergency_device_images;
DROP TABLE IF EXISTS emergency_device_vehicle_images;

CREATE TABLE emergency_device_images (
  device_id char(36) NOT NULL,
  position int NOT NULL,
  media_id char(36) NOT NULL,
  PRIMARY KEY (device_id, position),
  UNIQUE KEY uk_device_image_media (media_id),
  CONSTRAINT fk_device_image_device FOREIGN KEY (device_id) REFERENCES emergency_devices(id) ON DELETE CASCADE,
  CONSTRAINT fk_device_image_media FOREIGN KEY (media_id) REFERENCES media_assets(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE emergency_device_vehicle_images (
  device_id char(36) NOT NULL,
  position int NOT NULL,
  media_id char(36) NOT NULL,
  PRIMARY KEY (device_id, position),
  UNIQUE KEY uk_device_vehicle_image_media (media_id),
  CONSTRAINT fk_device_vehicle_image_device FOREIGN KEY (device_id) REFERENCES emergency_devices(id) ON DELETE CASCADE,
  CONSTRAINT fk_device_vehicle_image_media FOREIGN KEY (media_id) REFERENCES media_assets(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE science_submissions
  ADD COLUMN cover_media_id char(36) NULL AFTER has_cover_image,
  ADD CONSTRAINT fk_science_cover_media FOREIGN KEY (cover_media_id) REFERENCES media_assets(id) ON DELETE RESTRICT,
  DROP COLUMN cover_image_url;

UPDATE science_submissions SET has_cover_image = FALSE WHERE cover_media_id IS NULL;
