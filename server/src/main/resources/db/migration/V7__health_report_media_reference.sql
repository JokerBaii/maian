ALTER TABLE health_reports
  DROP COLUMN source_image_url,
  ADD COLUMN source_media_id char(36) NULL AFTER hospital,
  ADD KEY idx_health_report_source_media (source_media_id),
  ADD CONSTRAINT fk_health_report_source_media
    FOREIGN KEY (source_media_id) REFERENCES media_assets(id) ON DELETE SET NULL;
