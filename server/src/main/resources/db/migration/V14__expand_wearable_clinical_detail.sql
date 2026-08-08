-- 将穿戴设备记录扩展为分钟级当日轨迹和十分钟级历史轨迹。
-- 所有时间先按 Asia/Shanghai 构造，再转换为 UTC 持久化。

SET @wearable_local_now = CONVERT_TZ(UTC_TIMESTAMP(6), '+00:00', '+08:00');
SET @wearable_local_today = DATE(@wearable_local_now);

-- 展示账号采用健康稳定基线，移除既有极端采样；告警规则本身仍由服务测试覆盖。
DELETE FROM heart_rate_readings
WHERE user_id IN (
  '30000000-0000-0000-0000-000000000001',
  '30000000-0000-0000-0000-000000000002',
  '30000000-0000-0000-0000-000000000003'
)
AND recorded_at >= CONVERT_TZ(DATE_SUB(@wearable_local_today, INTERVAL 29 DAY), '+08:00', '+00:00')
AND (bpm < 58 OR bpm > 118);

-- V13 已写入整点和半点，本次补齐每小时 10、20、40、50 分的数据。
INSERT INTO heart_rate_readings (user_id, wearable_id, bpm, scene, recorded_at)
WITH RECURSIVE days(d) AS (
  SELECT 1 UNION ALL SELECT d + 1 FROM days WHERE d < 29
), half_hours(n) AS (
  SELECT 0 UNION ALL SELECT n + 1 FROM half_hours WHERE n < 47
), offsets(m) AS (
  SELECT 10 UNION ALL SELECT 20
)
SELECT
  users.user_id,
  users.wearable_id,
  CASE
    WHEN FLOOR(half_hours.n / 2) < 6
      THEN 58 + MOD(days.d * 3 + half_hours.n * 5 + offsets.m + users.offset_bpm, 11)
    WHEN FLOOR(half_hours.n / 2) < 9
      THEN 66 + MOD(days.d * 5 + half_hours.n * 3 + offsets.m + users.offset_bpm, 17)
    WHEN FLOOR(half_hours.n / 2) < 17
      THEN 72 + MOD(days.d * 7 + half_hours.n * 4 + offsets.m + users.offset_bpm, 22)
    WHEN FLOOR(half_hours.n / 2) < 19
      THEN 102 + MOD(days.d * 5 + half_hours.n * 7 + offsets.m + users.offset_bpm, 17)
    WHEN FLOOR(half_hours.n / 2) < 23
      THEN 70 + MOD(days.d * 4 + half_hours.n * 3 + offsets.m + users.offset_bpm, 18)
    ELSE 59 + MOD(days.d * 2 + half_hours.n * 5 + offsets.m + users.offset_bpm, 12)
  END,
  CASE
    WHEN FLOOR(half_hours.n / 2) < 6 THEN 'sleeping'
    WHEN FLOOR(half_hours.n / 2) BETWEEN 17 AND 18 THEN 'exercise'
    ELSE 'resting'
  END,
  CONVERT_TZ(
    DATE_SUB(@wearable_local_today, INTERVAL days.d DAY)
      + INTERVAL (half_hours.n * 30 + offsets.m) MINUTE,
    '+08:00', '+00:00'
  )
FROM (
  SELECT '30000000-0000-0000-0000-000000000001' user_id,
         '70000000-0000-0000-0000-000000000001' wearable_id, 0 offset_bpm
  UNION ALL SELECT '30000000-0000-0000-0000-000000000002',
                   '70000000-0000-0000-0000-000000000002', 3
  UNION ALL SELECT '30000000-0000-0000-0000-000000000003',
                   '70000000-0000-0000-0000-000000000003', 1
) users
CROSS JOIN days
CROSS JOIN half_hours
CROSS JOIN offsets;

-- 当日补齐到每分钟采样；5 分钟整数点已由 V13 写入，避免重复。
INSERT INTO heart_rate_readings (user_id, wearable_id, bpm, scene, recorded_at)
WITH RECURSIVE minute_slots(n) AS (
  SELECT 0
  UNION ALL
  SELECT n + 1 FROM minute_slots
  WHERE n < TIMESTAMPDIFF(MINUTE, @wearable_local_today, @wearable_local_now)
)
SELECT
  users.user_id,
  users.wearable_id,
  CASE
    WHEN HOUR(@wearable_local_today + INTERVAL minute_slots.n MINUTE) < 6
      THEN 58 + MOD(minute_slots.n * 5 + users.offset_bpm, 11)
    WHEN HOUR(@wearable_local_today + INTERVAL minute_slots.n MINUTE) < 9
      THEN 66 + MOD(minute_slots.n * 3 + users.offset_bpm, 16)
    WHEN HOUR(@wearable_local_today + INTERVAL minute_slots.n MINUTE) < 17
      THEN 72 + MOD(minute_slots.n * 7 + users.offset_bpm, 20)
    WHEN HOUR(@wearable_local_today + INTERVAL minute_slots.n MINUTE) < 19
      THEN 102 + MOD(minute_slots.n * 5 + users.offset_bpm, 17)
    WHEN HOUR(@wearable_local_today + INTERVAL minute_slots.n MINUTE) < 23
      THEN 69 + MOD(minute_slots.n * 4 + users.offset_bpm, 18)
    ELSE 59 + MOD(minute_slots.n * 3 + users.offset_bpm, 11)
  END,
  CASE
    WHEN HOUR(@wearable_local_today + INTERVAL minute_slots.n MINUTE) < 6 THEN 'sleeping'
    WHEN HOUR(@wearable_local_today + INTERVAL minute_slots.n MINUTE) BETWEEN 17 AND 18 THEN 'exercise'
    ELSE 'resting'
  END,
  CONVERT_TZ(
    @wearable_local_today + INTERVAL minute_slots.n MINUTE,
    '+08:00', '+00:00'
  )
FROM (
  SELECT '30000000-0000-0000-0000-000000000001' user_id,
         '70000000-0000-0000-0000-000000000001' wearable_id, 0 offset_bpm
  UNION ALL SELECT '30000000-0000-0000-0000-000000000002',
                   '70000000-0000-0000-0000-000000000002', 3
  UNION ALL SELECT '30000000-0000-0000-0000-000000000003',
                   '70000000-0000-0000-0000-000000000003', 1
) users
CROSS JOIN minute_slots
WHERE MOD(minute_slots.n, 5) <> 0;

UPDATE wearable_devices
SET last_seen_at = UTC_TIMESTAMP(6), updated_at = UTC_TIMESTAMP(6)
WHERE connected = TRUE;

INSERT INTO health_report_indicators
  (report_id, position, name, value_text, unit, reference_range, abnormal) VALUES
  ('a6000000-0000-0000-0000-000000000001', 12, '白细胞计数', '6.2', '10⁹/L', '3.5-9.5', FALSE),
  ('a6000000-0000-0000-0000-000000000001', 13, '血小板计数', '226', '10⁹/L', '125-350', FALSE),
  ('a6000000-0000-0000-0000-000000000001', 14, '谷草转氨酶', '21', 'U/L', '15-40', FALSE),
  ('a6000000-0000-0000-0000-000000000001', 15, '尿素', '5.1', 'mmol/L', '3.1-8.0', FALSE),
  ('a6000000-0000-0000-0000-000000000001', 16, '肾小球滤过率', '104', 'mL/min', '≥90', FALSE),
  ('a6000000-0000-0000-0000-000000000001', 17, '血氧饱和度', '98', '%', '95-100', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 12, '白细胞计数', '5.8', '10⁹/L', '3.5-9.5', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 13, '血小板计数', '241', '10⁹/L', '125-350', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 14, '谷草转氨酶', '24', 'U/L', '15-40', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 15, '尿素', '5.4', 'mmol/L', '3.1-8.0', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 16, '肾小球滤过率', '101', 'mL/min', '≥90', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 17, '丙氨酸氨基转移酶', '28', 'U/L', '9-50', FALSE);

INSERT INTO health_report_recommendations (report_id, position, recommendation) VALUES
  ('a6000000-0000-0000-0000-000000000001', 4, '保证每日饮水与蔬菜摄入，减少连续久坐时间'),
  ('a6000000-0000-0000-0000-000000000001', 5, '若连续出现心率异常并伴胸闷、眩晕或气促，应及时就医'),
  ('a6000000-0000-0000-0000-000000000004', 4, '训练日关注睡眠时长、静息心率与疲劳恢复的共同变化'),
  ('a6000000-0000-0000-0000-000000000004', 5, '连续高心率或恢复时间明显延长时应降低训练强度');

UPDATE health_reports
SET summary = '本次共评估 18 项指标。血压与空腹血糖处于参考范围，血脂结构平稳；血常规未见明显异常，肝酶、肌酐、尿素及肾小球滤过率提示肝肾功能状态良好。静息心率与血氧表现稳定，建议结合手环的睡眠、静息和运动心率持续观察长期变化。'
WHERE id = 'a6000000-0000-0000-0000-000000000001';

UPDATE health_reports
SET summary = '本次共评估 18 项指标。血压、血糖、血脂、血常规及肝肾功能均在参考范围；静息心率、血氧和运动后恢复心率表现良好。建议在训练日持续记录恢复速度、睡眠质量与静息心率，避免在疲劳状态下突然增加运动强度。'
WHERE id = 'a6000000-0000-0000-0000-000000000004';
