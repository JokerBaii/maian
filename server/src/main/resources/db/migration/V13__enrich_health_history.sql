-- 为健康趋势提供连续、可比较的近 30 天记录，并补全报告指标。
-- 时间统一按 Asia/Shanghai 生成，再转换为 UTC 持久化。

SET @health_local_now = CONVERT_TZ(UTC_TIMESTAMP(6), '+00:00', '+08:00');
SET @health_local_today = DATE(@health_local_now);

UPDATE wearable_devices
SET connected = TRUE,
    battery = CASE user_id
      WHEN '30000000-0000-0000-0000-000000000001' THEN 86
      WHEN '30000000-0000-0000-0000-000000000002' THEN 88
      ELSE 91
    END,
    last_seen_at = UTC_TIMESTAMP(6),
    updated_at = UTC_TIMESTAMP(6)
WHERE user_id IN (
  '30000000-0000-0000-0000-000000000001',
  '30000000-0000-0000-0000-000000000002',
  '30000000-0000-0000-0000-000000000003'
);

-- 过去 29 个完整自然日，每 30 分钟一条记录。
INSERT INTO heart_rate_readings (user_id, wearable_id, bpm, scene, recorded_at)
WITH RECURSIVE days(d) AS (
  SELECT 1 UNION ALL SELECT d + 1 FROM days WHERE d < 29
), slots(n) AS (
  SELECT 0 UNION ALL SELECT n + 1 FROM slots WHERE n < 47
)
SELECT
  users.user_id,
  users.wearable_id,
  CASE
    WHEN FLOOR(slots.n / 2) < 6 THEN 55 + MOD(days.d * 3 + slots.n * 5 + users.offset_bpm, 12)
    WHEN FLOOR(slots.n / 2) < 9 THEN 66 + MOD(days.d * 5 + slots.n * 3 + users.offset_bpm, 17)
    WHEN FLOOR(slots.n / 2) < 17 THEN 72 + MOD(days.d * 7 + slots.n * 4 + users.offset_bpm, 22)
    WHEN FLOOR(slots.n / 2) < 19 THEN 102 + MOD(days.d * 5 + slots.n * 7 + users.offset_bpm, 23)
    WHEN FLOOR(slots.n / 2) < 23 THEN 70 + MOD(days.d * 4 + slots.n * 3 + users.offset_bpm, 18)
    ELSE 59 + MOD(days.d * 2 + slots.n * 5 + users.offset_bpm, 12)
  END,
  CASE
    WHEN FLOOR(slots.n / 2) < 6 THEN 'sleeping'
    WHEN FLOOR(slots.n / 2) BETWEEN 17 AND 18 THEN 'exercise'
    ELSE 'resting'
  END,
  CONVERT_TZ(
    DATE_SUB(@health_local_today, INTERVAL days.d DAY) + INTERVAL (slots.n * 30) MINUTE,
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
CROSS JOIN slots;

-- 当日从零点到当前时刻每 5 分钟一条，保证实时页具备足够的趋势细节。
INSERT INTO heart_rate_readings (user_id, wearable_id, bpm, scene, recorded_at)
WITH RECURSIVE slots(n) AS (
  SELECT 0
  UNION ALL
  SELECT n + 1 FROM slots
  WHERE n < FLOOR(TIMESTAMPDIFF(MINUTE, @health_local_today, @health_local_now) / 5)
)
SELECT
  users.user_id,
  users.wearable_id,
  CASE
    WHEN HOUR(@health_local_today + INTERVAL (slots.n * 5) MINUTE) < 6
      THEN 56 + MOD(slots.n * 5 + users.offset_bpm, 11)
    WHEN HOUR(@health_local_today + INTERVAL (slots.n * 5) MINUTE) < 9
      THEN 66 + MOD(slots.n * 3 + users.offset_bpm, 16)
    WHEN HOUR(@health_local_today + INTERVAL (slots.n * 5) MINUTE) < 17
      THEN 72 + MOD(slots.n * 7 + users.offset_bpm, 20)
    WHEN HOUR(@health_local_today + INTERVAL (slots.n * 5) MINUTE) < 19
      THEN 102 + MOD(slots.n * 5 + users.offset_bpm, 20)
    WHEN HOUR(@health_local_today + INTERVAL (slots.n * 5) MINUTE) < 23
      THEN 69 + MOD(slots.n * 4 + users.offset_bpm, 18)
    ELSE 59 + MOD(slots.n * 3 + users.offset_bpm, 11)
  END,
  CASE
    WHEN HOUR(@health_local_today + INTERVAL (slots.n * 5) MINUTE) < 6 THEN 'sleeping'
    WHEN HOUR(@health_local_today + INTERVAL (slots.n * 5) MINUTE) BETWEEN 17 AND 18 THEN 'exercise'
    ELSE 'resting'
  END,
  CONVERT_TZ(
    @health_local_today + INTERVAL (slots.n * 5) MINUTE,
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
CROSS JOIN slots;

-- 连续异常片段用于验证持续时间、迟滞和冷却规则，不以单个偶发点触发预警。
INSERT INTO heart_rate_readings (user_id, wearable_id, bpm, scene, recorded_at) VALUES
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 123, 'exercise', CONVERT_TZ(DATE_SUB(@health_local_today, INTERVAL 1 DAY) + INTERVAL 18 HOUR + INTERVAL 20 MINUTE, '+08:00', '+00:00')),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 127, 'exercise', CONVERT_TZ(DATE_SUB(@health_local_today, INTERVAL 1 DAY) + INTERVAL 18 HOUR + INTERVAL 20 MINUTE + INTERVAL 30 SECOND, '+08:00', '+00:00')),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 131, 'exercise', CONVERT_TZ(DATE_SUB(@health_local_today, INTERVAL 1 DAY) + INTERVAL 18 HOUR + INTERVAL 21 MINUTE, '+08:00', '+00:00')),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 125, 'exercise', CONVERT_TZ(DATE_SUB(@health_local_today, INTERVAL 1 DAY) + INTERVAL 18 HOUR + INTERVAL 21 MINUTE + INTERVAL 30 SECOND, '+08:00', '+00:00')),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 112, 'resting', CONVERT_TZ(DATE_SUB(@health_local_today, INTERVAL 1 DAY) + INTERVAL 18 HOUR + INTERVAL 23 MINUTE, '+08:00', '+00:00')),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 47, 'sleeping', CONVERT_TZ(DATE_SUB(@health_local_today, INTERVAL 3 DAY) + INTERVAL 4 HOUR + INTERVAL 10 MINUTE, '+08:00', '+00:00')),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 45, 'sleeping', CONVERT_TZ(DATE_SUB(@health_local_today, INTERVAL 3 DAY) + INTERVAL 4 HOUR + INTERVAL 10 MINUTE + INTERVAL 30 SECOND, '+08:00', '+00:00')),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 46, 'sleeping', CONVERT_TZ(DATE_SUB(@health_local_today, INTERVAL 3 DAY) + INTERVAL 4 HOUR + INTERVAL 11 MINUTE, '+08:00', '+00:00')),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 48, 'sleeping', CONVERT_TZ(DATE_SUB(@health_local_today, INTERVAL 3 DAY) + INTERVAL 4 HOUR + INTERVAL 11 MINUTE + INTERVAL 30 SECOND, '+08:00', '+00:00')),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 57, 'sleeping', CONVERT_TZ(DATE_SUB(@health_local_today, INTERVAL 3 DAY) + INTERVAL 4 HOUR + INTERVAL 13 MINUTE, '+08:00', '+00:00'));

INSERT INTO health_report_indicators
  (report_id, position, name, value_text, unit, reference_range, abnormal) VALUES
  ('a6000000-0000-0000-0000-000000000001', 5, '甘油三酯', '1.26', 'mmol/L', '0.45-1.70', FALSE),
  ('a6000000-0000-0000-0000-000000000001', 6, '低密度脂蛋白', '2.61', 'mmol/L', '0.00-3.37', FALSE),
  ('a6000000-0000-0000-0000-000000000001', 7, '高密度脂蛋白', '1.42', 'mmol/L', '1.04-1.55', FALSE),
  ('a6000000-0000-0000-0000-000000000001', 8, '血红蛋白', '143', 'g/L', '130-175', FALSE),
  ('a6000000-0000-0000-0000-000000000001', 9, '谷丙转氨酶', '24', 'U/L', '9-50', FALSE),
  ('a6000000-0000-0000-0000-000000000001', 10, '血肌酐', '78', 'μmol/L', '57-97', FALSE),
  ('a6000000-0000-0000-0000-000000000001', 11, '尿酸', '356', 'μmol/L', '208-428', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 4, '空腹血糖', '5.0', 'mmol/L', '3.9-6.1', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 5, '总胆固醇', '4.3', 'mmol/L', '2.85-5.70', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 6, '甘油三酯', '1.08', 'mmol/L', '0.45-1.70', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 7, '血红蛋白', '151', 'g/L', '130-175', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 8, '谷丙转氨酶', '28', 'U/L', '9-50', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 9, '血肌酐', '82', 'μmol/L', '57-97', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 10, '尿酸', '371', 'μmol/L', '208-428', FALSE),
  ('a6000000-0000-0000-0000-000000000004', 11, '体重指数', '22.4', 'kg/m²', '18.5-23.9', FALSE);

INSERT INTO health_report_recommendations (report_id, position, recommendation) VALUES
  ('a6000000-0000-0000-0000-000000000001', 2, '每周固定时间测量血压，并结合手环静息心率观察长期趋势'),
  ('a6000000-0000-0000-0000-000000000001', 3, '下一次体检继续复查血脂、血糖与肝肾功能'),
  ('a6000000-0000-0000-0000-000000000004', 2, '保持每周三至五次有氧活动，运动量循序增加'),
  ('a6000000-0000-0000-0000-000000000004', 3, '如运动后出现胸闷、眩晕或心悸，应停止活动并及时就医');

UPDATE health_reports
SET summary = '本次 12 项指标均处于参考范围，血压、血糖、血脂与肝肾功能整体平稳，建议结合手环趋势继续观察。'
WHERE id = 'a6000000-0000-0000-0000-000000000001';

UPDATE health_reports
SET summary = '本次 12 项指标整体平稳，运动耐量和恢复心率表现良好，可继续保持规律训练并关注恢复速度。'
WHERE id = 'a6000000-0000-0000-0000-000000000004';
