-- 补充健康监测演示数据。
--
-- V1/V2 的心率数据停留在固定日期，健康监测页的"今日趋势"长期为空。
-- 这里用相对时间（最近 7 天）为三个演示账号各生成一条合理的每日曲线：
--   00:30 夜间静息（58-70）→ 07:30 晨起（74-88）→ 09:00 上午活动（80-95）
--   → 12:30 午后（76-88）→ 15:00 下午活动（82-96）→ 18:30 傍晚运动（105-130，可触发预警）
--   → 21:00 晚间（74-88）→ 23:30 睡前（60-70）
-- 并给志愿者账号补一台穿戴设备。
--
-- bpm 用确定性伪随机（d/n 取模）保证每次执行结果一致、可复现。

-- ============ 志愿者账号补穿戴设备 ============
INSERT INTO wearable_devices (
  id, user_id, device_identifier, name, type, connected, battery, last_seen_at, created_at, updated_at
) VALUES (
  '70000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002',
  'pulsecare-watch-s2', 'PulseCare Watch S2', 'watch', 1, 88,
  UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
);

-- ============ 三用户最近 7 天心率曲线 ============
INSERT INTO heart_rate_readings (user_id, wearable_id, bpm, scene, recorded_at)
WITH RECURSIVE days(d) AS (
  SELECT 0 UNION ALL SELECT d + 1 FROM days WHERE d < 6
), slots(n) AS (
  SELECT 0 UNION ALL SELECT n + 1 FROM slots WHERE n < 7
)
SELECT
  users.user_id,
  users.wearable_id,
  CASE slots.n
    WHEN 0 THEN 58 + ((days.d * 3 + slots.n * 5) % 12)          -- 00:30 夜间静息
    WHEN 1 THEN 74 + ((days.d * 2 + slots.n * 4) % 12)          -- 07:30 晨起
    WHEN 2 THEN 80 + ((days.d * 5 + slots.n * 3) % 14)          -- 09:00 上午活动
    WHEN 3 THEN 76 + ((days.d * 4 + slots.n * 6) % 12)          -- 12:30 午后
    WHEN 4 THEN 82 + ((days.d * 6 + slots.n * 2) % 14)          -- 15:00 下午活动
    WHEN 5 THEN 105 + ((days.d * 7 + slots.n * 9) % 26)         -- 18:30 傍晚运动（部分超过 120 阈值）
    WHEN 6 THEN 74 + ((days.d * 3 + slots.n * 5) % 12)          -- 21:00 晚间
    ELSE 60 + ((days.d * 2 + slots.n * 3) % 10)                 -- 23:30 睡前
  END + users.offset_bpm AS bpm,
  CASE WHEN slots.n IN (2, 4) THEN 'walking'
       WHEN slots.n = 5 THEN 'exercise'
       ELSE 'resting' END AS scene,
  DATE_SUB(UTC_TIMESTAMP(6), INTERVAL days.d DAY)
    + INTERVAL (CASE slots.n
        WHEN 0 THEN 0 WHEN 1 THEN 7 WHEN 2 THEN 9 WHEN 3 THEN 12
        WHEN 4 THEN 15 WHEN 5 THEN 18 WHEN 6 THEN 21 ELSE 23 END) HOUR
    + INTERVAL (CASE WHEN slots.n IN (0, 1, 3, 5, 7) THEN 30 ELSE 0 END) MINUTE AS recorded_at
FROM (
  SELECT '30000000-0000-0000-0000-000000000001' AS user_id,
         '70000000-0000-0000-0000-000000000001' AS wearable_id, 0 AS offset_bpm
  UNION ALL
  SELECT '30000000-0000-0000-0000-000000000002',
         '70000000-0000-0000-0000-000000000002', 3
  UNION ALL
  SELECT '30000000-0000-0000-0000-000000000003',
         '70000000-0000-0000-0000-000000000003', -2
) users
CROSS JOIN days
CROSS JOIN slots;

-- ============ 用户1 今日额外超阈值记录，保证预警页有内容 ============
INSERT INTO heart_rate_readings (user_id, wearable_id, bpm, scene, recorded_at) VALUES
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001',
   128, 'exercise', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 HOUR)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001',
   133, 'walking', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 HOUR));
