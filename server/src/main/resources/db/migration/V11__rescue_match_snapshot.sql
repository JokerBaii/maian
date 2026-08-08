ALTER TABLE rescue_calls
  ADD COLUMN matched_snapshot_latitude double NULL AFTER estimated_arrival_seconds,
  ADD COLUMN matched_snapshot_longitude double NULL AFTER matched_snapshot_latitude,
  ADD COLUMN matched_snapshot_address varchar(255) NULL AFTER matched_snapshot_longitude;

UPDATE rescue_calls call_record
JOIN emergency_devices device ON device.id = call_record.matched_device_id
SET call_record.matched_snapshot_latitude = device.latitude,
    call_record.matched_snapshot_longitude = device.longitude,
    call_record.matched_snapshot_address = device.address
WHERE call_record.matched_device_id IS NOT NULL;
