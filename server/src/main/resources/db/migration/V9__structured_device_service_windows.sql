CREATE TABLE emergency_device_service_windows (
  device_id char(36) NOT NULL,
  position int NOT NULL,
  day_of_week varchar(12) NOT NULL,
  opens_at time NOT NULL,
  closes_at time NOT NULL,
  PRIMARY KEY (device_id, position),
  KEY idx_device_service_day (day_of_week, opens_at, closes_at),
  CONSTRAINT fk_device_service_window
    FOREIGN KEY (device_id) REFERENCES emergency_devices(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Only reliably parseable legacy schedules are migrated. Unknown text intentionally
-- becomes an empty schedule and is therefore excluded from dispatch.
INSERT INTO emergency_device_service_windows (
  device_id, position, day_of_week, opens_at, closes_at
)
SELECT
  device.id,
  week_day.position,
  week_day.day_of_week,
  CASE
    WHEN device.service_time LIKE '%全天%' THEN CAST('00:00:00' AS TIME)
    ELSE STR_TO_DATE(REGEXP_SUBSTR(device.service_time, '[0-9]{1,2}:[0-9]{2}', 1, 1), '%H:%i')
  END,
  CASE
    WHEN device.service_time LIKE '%全天%' THEN CAST('00:00:00' AS TIME)
    ELSE STR_TO_DATE(REGEXP_SUBSTR(device.service_time, '[0-9]{1,2}:[0-9]{2}', 1, 2), '%H:%i')
  END
FROM emergency_devices device
CROSS JOIN (
  SELECT 0 AS position, 'MONDAY' AS day_of_week
  UNION ALL SELECT 1, 'TUESDAY'
  UNION ALL SELECT 2, 'WEDNESDAY'
  UNION ALL SELECT 3, 'THURSDAY'
  UNION ALL SELECT 4, 'FRIDAY'
  UNION ALL SELECT 5, 'SATURDAY'
  UNION ALL SELECT 6, 'SUNDAY'
) week_day
WHERE (
    device.service_time LIKE '%全天%'
    OR device.service_time REGEXP '[0-9]{1,2}:[0-9]{2}.*[0-9]{1,2}:[0-9]{2}'
  )
  AND (device.service_time NOT LIKE '%工作日%' OR week_day.position <= 4);

ALTER TABLE emergency_devices DROP COLUMN service_time;
