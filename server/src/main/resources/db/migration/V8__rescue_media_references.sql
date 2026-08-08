DROP TABLE IF EXISTS rescue_call_images;

CREATE TABLE rescue_call_media (
  rescue_call_id char(36) NOT NULL,
  position int NOT NULL,
  media_id char(36) NOT NULL,
  PRIMARY KEY (rescue_call_id, position),
  UNIQUE KEY uk_rescue_media_asset (media_id),
  CONSTRAINT fk_rescue_media_call FOREIGN KEY (rescue_call_id) REFERENCES rescue_calls(id) ON DELETE CASCADE,
  CONSTRAINT fk_rescue_media_asset FOREIGN KEY (media_id) REFERENCES media_assets(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
