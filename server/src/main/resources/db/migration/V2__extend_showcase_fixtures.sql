-- 补充展示数据，覆盖三个角色的完整业务视角。
--
-- V1 的种子数据缺少两块，导致部分流程无法完整走通：
--   1. 没有处于 PENDING/MATCHING 的呼救，志愿者的"救援任务"列表为空，无法演示接单；
--   2. 志愿者和审核员没有紧急联系人，个人页信息不完整。
-- 这里补齐上述数据，并增加待审核设备与体检报告，使各角色都有可展示内容。

SET @user_id   = '30000000-0000-0000-0000-000000000001';
SET @vol_id    = '30000000-0000-0000-0000-000000000002';
SET @admin_id  = '30000000-0000-0000-0000-000000000003';

-- ============ 待接单呼救：志愿者接单演示的前提 ============
INSERT INTO rescue_calls (
  id, urgency, status, latitude, longitude, address, description,
  created_at, updated_at, matched_device_id, matched_at,
  matched_distance_meters, estimated_arrival_seconds, match_strategy,
  client_request_id, requested_by_user_id, responder_user_id,
  accepted_at, completed_at
) VALUES
  ('41000000-0000-0000-0000-000000000001', 'CRITICAL', 'MATCHING', 30.2748, 120.1445,
   '黄龙体育馆东门广场', '男性约 55 岁突然倒地，呼叫无回应，现场已有人协助疏散。',
   DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 MINUTE), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 MINUTE),
   '10000000-0000-0000-0000-000000000006', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 MINUTE),
   168, 214, 'ETA_V2_FIXED_RETRIEVAL', 'seed-open-001',
   @user_id, NULL, NULL, NULL),
  ('41000000-0000-0000-0000-000000000002', 'HIGH', 'MATCHING', 30.2762, 120.1648,
   '武林广场地铁站 B 口', '女性突发胸闷伴出汗，意识清醒，已就近安置休息。',
   DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 9 MINUTE), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 9 MINUTE),
   '10000000-0000-0000-0000-000000000007', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 9 MINUTE),
   132, 176, 'ETA_V2_FIXED_RETRIEVAL', 'seed-open-002',
   @user_id, NULL, NULL, NULL),
  ('41000000-0000-0000-0000-000000000003', 'MEDIUM', 'PENDING', 30.2951, 120.1205,
   '丰潭路商业中心一层中庭', '老人行走时扭伤脚踝，无法自行站起，需要急救包协助处理。',
   DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 15 MINUTE), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 15 MINUTE),
   NULL, NULL, NULL, NULL, NULL, 'seed-open-003',
   @user_id, NULL, NULL, NULL);

INSERT INTO rescue_call_symptoms (rescue_call_id, symptom) VALUES
  ('41000000-0000-0000-0000-000000000001', '意识丧失'),
  ('41000000-0000-0000-0000-000000000001', '呼吸异常'),
  ('41000000-0000-0000-0000-000000000002', '胸闷胸痛'),
  ('41000000-0000-0000-0000-000000000003', '外伤扭伤');

-- 一条进行中的救援，用于展示"救援中"状态与历史记录的区分
INSERT INTO rescue_calls (
  id, urgency, status, latitude, longitude, address, description,
  created_at, updated_at, matched_device_id, matched_at,
  matched_distance_meters, estimated_arrival_seconds, match_strategy,
  client_request_id, requested_by_user_id, responder_user_id,
  accepted_at, completed_at
) VALUES
  ('41000000-0000-0000-0000-000000000004', 'HIGH', 'RESCUING', 30.2705, 120.1452,
   '浙江图书馆一楼大厅', '读者突发晕厥，志愿者已到场并取回 AED，正在配合现场处置。',
   DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 22 MINUTE), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 MINUTE),
   '10000000-0000-0000-0000-000000000009', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 21 MINUTE),
   96, 148, 'ETA_V2_FIXED_RETRIEVAL', 'seed-open-004',
   @user_id, @vol_id,
   DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 18 MINUTE), NULL);

INSERT INTO rescue_call_symptoms (rescue_call_id, symptom) VALUES
  ('41000000-0000-0000-0000-000000000004', '意识丧失');

-- ============ 紧急联系人：补齐志愿者与审核员 ============
INSERT INTO emergency_contacts (
  id, user_id, name, phone, relation_name, created_at
) VALUES
  ('31000000-0000-0000-0000-000000000005', @vol_id, '队务调度台', '057188002120', '志愿服务队', UTC_TIMESTAMP(6)),
  ('31000000-0000-0000-0000-000000000006', @vol_id, '陈女士', '13600000000', '家属', UTC_TIMESTAMP(6)),
  ('31000000-0000-0000-0000-000000000007', @admin_id, '平台值班室', '057188003120', '平台运营', UTC_TIMESTAMP(6)),
  ('31000000-0000-0000-0000-000000000008', @admin_id, '孙先生', '13500000000', '家属', UTC_TIMESTAMP(6));

-- ============ 待审核设备：审核队列扩充到 4 条 ============
INSERT INTO emergency_devices (
  id, type, category, name, address, longitude, latitude, status,
  owner_phone, service_time, created_at, expire_date, owner, vehicle_info,
  service_range, instructions, last_location_at, version,
  registered_by_user_id, review_note, reviewed_at
) VALUES
  ('90000000-0000-0000-0000-000000000002', 'FIXED', 'AED', '文三路写字楼 AED', '文三路数码大厦一层前台', 120.1288, 30.2812, 'PENDING_REVIEW', NULL, '08:30-19:00', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 HOUR), '2029-07-31', '大厦物业中心', NULL, NULL, '一层前台后方设备柜', NULL, 0, @user_id, NULL, NULL),
  ('90000000-0000-0000-0000-000000000003', 'FIXED', '急救箱', '之江路健身中心急救箱', '之江路全民健身中心二层', 120.1522, 30.2085, 'PENDING_REVIEW', NULL, '09:00-21:30', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 HOUR), '2028-11-30', '健身中心运营方', NULL, NULL, '二层教练值班台领取', NULL, 0, @user_id, NULL, NULL),
  ('90000000-0000-0000-0000-000000000004', 'MOBILE', 'AED', '余杭流动 AED', '当前位于文一西路附近', 120.0421, 30.2831, 'PENDING_REVIEW', NULL, '08:00-20:00', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 HOUR), '2029-10-31', '余杭应急志愿服务队', '白色新能源轿车', 6, '通过审核后参与调度', UTC_TIMESTAMP(6), 0, @vol_id, NULL, NULL);

-- ============ 体检报告：为体检档案与趋势对比补充历史记录 ============
INSERT INTO health_reports (
  id, user_id, checkup_date, hospital, source_image_url,
  risk_level, summary, disclaimer, analysis_source, created_at
) VALUES
  ('61000000-0000-0000-0000-000000000001', @user_id,
   DATE_SUB(UTC_DATE(), INTERVAL 210 DAY), '杭州市第一人民医院体检中心', NULL,
   'LOW', '本次报告各项指标均在参考范围内，建议保持规律监测。',
   '本结果仅用于健康信息整理，不能替代医生诊断；如有胸痛、呼吸困难或意识异常，请立即呼叫急救。',
   'RULE_BASED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 210 DAY)),
  ('61000000-0000-0000-0000-000000000002', @user_id,
   DATE_SUB(UTC_DATE(), INTERVAL 95 DAY), '浙江省人民医院健康管理中心', NULL,
   'HIGH', '本次报告发现 4 项指标超出参考范围，建议尽快就医评估。',
   '本结果仅用于健康信息整理，不能替代医生诊断；如有胸痛、呼吸困难或意识异常，请立即呼叫急救。',
   'RULE_BASED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 95 DAY));

INSERT INTO health_report_indicators (report_id, position, name, value_text, unit, reference_range, abnormal) VALUES
  ('61000000-0000-0000-0000-000000000001', 0, '血压', '118/76', 'mmHg', '90-139/60-89', FALSE),
  ('61000000-0000-0000-0000-000000000001', 1, '空腹血糖', '5.1', 'mmol/L', '3.9-6.1', FALSE),
  ('61000000-0000-0000-0000-000000000001', 2, '总胆固醇', '4.6', 'mmol/L', '2.85-5.70', FALSE),
  ('61000000-0000-0000-0000-000000000001', 3, '血红蛋白', '145', 'g/L', '130-175', FALSE),
  ('61000000-0000-0000-0000-000000000002', 0, '血压', '148/95', 'mmHg', '90-139/60-89', TRUE),
  ('61000000-0000-0000-0000-000000000002', 1, '空腹血糖', '6.7', 'mmol/L', '3.9-6.1', TRUE),
  ('61000000-0000-0000-0000-000000000002', 2, '总胆固醇', '6.4', 'mmol/L', '2.85-5.70', TRUE),
  ('61000000-0000-0000-0000-000000000002', 3, '低密度脂蛋白', '4.12', 'mmol/L', '0.00-3.37', TRUE),
  ('61000000-0000-0000-0000-000000000002', 4, '静息心率', '86', '次/分', '60-100', FALSE);

INSERT INTO health_report_recommendations (report_id, position, recommendation) VALUES
  ('61000000-0000-0000-0000-000000000001', 0, '保持规律作息和适量运动'),
  ('61000000-0000-0000-0000-000000000001', 1, '按既定周期复查健康指标'),
  ('61000000-0000-0000-0000-000000000002', 0, '记录异常指标出现的时间和伴随症状'),
  ('61000000-0000-0000-0000-000000000002', 1, '携带完整报告咨询医生'),
  ('61000000-0000-0000-0000-000000000002', 2, '症状明显加重时及时就医');
