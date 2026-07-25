ALTER TABLE emergency_devices
    ADD COLUMN expire_date DATE NULL,
    ADD COLUMN owner VARCHAR(120) NULL,
    ADD COLUMN vehicle_info VARCHAR(120) NULL,
    ADD COLUMN service_range INT NULL,
    ADD COLUMN instructions VARCHAR(500) NULL;

UPDATE emergency_devices
SET owner = '城市公共急救网络',
    expire_date = CASE WHEN type = 'FIXED' THEN DATE_ADD(CURRENT_DATE, INTERVAL 2 YEAR) ELSE NULL END,
    vehicle_info = CASE WHEN type = 'MOBILE' THEN '志愿者便携设备' ELSE NULL END,
    service_range = CASE WHEN type = 'MOBILE' THEN 5 ELSE NULL END;
