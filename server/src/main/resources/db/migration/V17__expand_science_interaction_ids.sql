-- 审核通过的投稿使用 submission-{uuid} 作为前端文章编号，长度为 47。
-- 统一放宽交互表约束，确保内置科普与投稿文章共用同一套点赞/收藏链路。
ALTER TABLE science_article_interactions
  MODIFY COLUMN article_id VARCHAR(80) NOT NULL;

-- 头像属于账户资料，不再由前端按身份硬编码。
ALTER TABLE user_profiles
  ADD COLUMN avatar_url VARCHAR(255) NULL AFTER role;

UPDATE user_profiles SET avatar_url = CASE id
  WHEN '30000000-0000-0000-0000-000000000001' THEN '/static/avatars/demo-requester.webp'
  WHEN '30000000-0000-0000-0000-000000000002' THEN '/static/avatars/demo-volunteer.webp'
  WHEN '30000000-0000-0000-0000-000000000003' THEN '/static/avatars/demo-reviewer.webp'
  ELSE avatar_url
END
WHERE id IN (
  '30000000-0000-0000-0000-000000000001',
  '30000000-0000-0000-0000-000000000002',
  '30000000-0000-0000-0000-000000000003'
);

-- 演示审核队列包含待审、通过和退回三种结果；通过的稿件可在科普页正常互动。
UPDATE science_submissions
SET status = 'APPROVED', review_note = '内容准确、表达清楚，准予发布',
    reviewed_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 16 HOUR)
WHERE id = '80000000-0000-0000-0000-000000000002';

UPDATE science_submissions
SET status = 'REJECTED', review_note = '请补充适用人群与风险提示后重新提交',
    reviewed_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 8 HOUR)
WHERE id = '80000000-0000-0000-0000-000000000003';

INSERT IGNORE INTO science_article_interactions
  (id, user_id, article_id, liked, collected, updated_at) VALUES
  ('82000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'S001', TRUE, TRUE, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY)),
  ('82000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', 'S002', TRUE, FALSE, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY)),
  ('82000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', 'S003', FALSE, TRUE, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 HOUR)),
  ('82000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000001', 'submission-80000000-0000-0000-0000-000000000002', TRUE, TRUE, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 HOUR));

-- 固定一条跨角色、可继续推进的演示救援。只有行进状态显示 ETA；
-- 志愿者点击到达现场后，三端随状态刷新并立即停止倒计时。
SET @demo_requester = '30000000-0000-0000-0000-000000000001';
SET @demo_volunteer = '30000000-0000-0000-0000-000000000002';
SET @demo_call = '41000000-0000-0000-0000-000000000004';
SET @old_demo_aed = '10000000-0000-0000-0000-000000000009';
SET @demo_aed = '10000000-0000-0000-0000-000000000006';

UPDATE emergency_devices
SET status = 'AVAILABLE', reserved_for_call_id = NULL, reserved_at = NULL, version = version + 1
WHERE id = @old_demo_aed AND reserved_for_call_id = @demo_call;

UPDATE rescue_calls
SET status = 'EN_ROUTE_TO_REQUESTER', urgency = 'HIGH',
    address = '浙江图书馆一楼大厅',
    description = '读者突发晕厥，志愿者已取到 AED，正在沿道路赶往现场。',
    matched_device_id = @demo_aed,
    matched_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 18 MINUTE),
    matched_distance_meters = 312,
    estimated_arrival_seconds = 148,
    match_strategy = 'ETA_V2_FIXED_RETRIEVAL',
    requested_by_user_id = @demo_requester,
    responder_user_id = @demo_volunteer,
    accepted_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 16 MINUTE),
    arrived_at_aed_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 12 MINUTE),
    arrived_at = NULL, rescue_started_at = NULL,
    completion_submitted_at = NULL, confirmation_deadline_at = NULL,
    completed_at = NULL, aed_returned_at = NULL,
    aed_custody_status = 'IN_CUSTODY',
    responder_latitude = 30.2717, responder_longitude = 120.1443,
    responder_location_at = UTC_TIMESTAMP(6),
    created_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 22 MINUTE),
    updated_at = UTC_TIMESTAMP(6), event_sequence = 5
WHERE id = @demo_call AND client_request_id = 'seed-open-004';

UPDATE emergency_devices
SET status = 'RESERVED', reserved_for_call_id = @demo_call,
    reserved_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 18 MINUTE), version = version + 1
WHERE id = @demo_aed;

DELETE FROM rescue_events WHERE rescue_call_id = @demo_call;
INSERT INTO rescue_events (id, rescue_call_id, sequence_no, type, actor_user_id, summary, created_at) VALUES
  ('42000000-0000-0000-0000-000000000041', @demo_call, 1, 'CREATED', @demo_requester, '呼救已发出', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 22 MINUTE)),
  ('42000000-0000-0000-0000-000000000042', @demo_call, 2, 'AED_ASSIGNED', NULL, '已匹配黄龙体育馆东门 AED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 18 MINUTE)),
  ('42000000-0000-0000-0000-000000000043', @demo_call, 3, 'ACCEPTED', @demo_volunteer, '志愿者已确认响应', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 16 MINUTE)),
  ('42000000-0000-0000-0000-000000000044', @demo_call, 4, 'ARRIVED_AT_AED', @demo_volunteer, '志愿者已到达 AED 取用点', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 12 MINUTE)),
  ('42000000-0000-0000-0000-000000000045', @demo_call, 5, 'AED_PICKED_UP', @demo_volunteer, 'AED 已取出，正在赶往求救者', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 10 MINUTE));

INSERT INTO active_rescue_locks (user_id, rescue_call_id, acquired_at)
VALUES (@demo_requester, @demo_call, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 22 MINUTE))
ON DUPLICATE KEY UPDATE rescue_call_id = VALUES(rescue_call_id), acquired_at = VALUES(acquired_at);

INSERT INTO responder_presence (user_id, latitude, longitude, available, updated_at)
VALUES (@demo_volunteer, 30.2717, 120.1443, TRUE, UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE latitude = VALUES(latitude), longitude = VALUES(longitude), available = TRUE, updated_at = VALUES(updated_at);
