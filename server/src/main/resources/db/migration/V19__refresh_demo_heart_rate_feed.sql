-- 演示穿戴设备需要持续产生近一小时采样，避免部署运行一段时间后趋势自然过期。
SET @demo_feed_now = UTC_TIMESTAMP(6);

DELETE FROM heart_rate_readings
WHERE user_id IN (
  '30000000-0000-0000-0000-000000000001',
  '30000000-0000-0000-0000-000000000002',
  '30000000-0000-0000-0000-000000000003'
)
AND recorded_at >= DATE_SUB(@demo_feed_now, INTERVAL 65 MINUTE);

INSERT INTO heart_rate_readings (user_id, wearable_id, bpm, scene, recorded_at)
WITH RECURSIVE slots(n) AS (
  SELECT 0 UNION ALL SELECT n + 1 FROM slots WHERE n < 11
)
SELECT
  users.user_id,
  users.wearable_id,
  72 + MOD(slots.n * 7 + users.offset_bpm, 17),
  'resting',
  DATE_SUB(@demo_feed_now, INTERVAL (slots.n * 5) MINUTE)
FROM (
  SELECT '30000000-0000-0000-0000-000000000001' user_id,
         '70000000-0000-0000-0000-000000000001' wearable_id, 0 offset_bpm
  UNION ALL SELECT '30000000-0000-0000-0000-000000000002',
                   '70000000-0000-0000-0000-000000000002', 3
  UNION ALL SELECT '30000000-0000-0000-0000-000000000003',
                   '70000000-0000-0000-0000-000000000003', 1
) users
CROSS JOIN slots;

UPDATE wearable_devices
SET last_seen_at = @demo_feed_now, updated_at = @demo_feed_now
WHERE user_id IN (
  '30000000-0000-0000-0000-000000000001',
  '30000000-0000-0000-0000-000000000002',
  '30000000-0000-0000-0000-000000000003'
)
AND connected = TRUE;
