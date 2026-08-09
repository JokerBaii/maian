-- 一条跨角色共享的演示救援：求救者查看进展、志愿者推进任务、审核员查看被调度设备。
-- 数据仅用于 demo profile，不代表真实事件或真实个人。

SET @demo_requester = '30000000-0000-0000-0000-000000000001';
SET @demo_volunteer = '30000000-0000-0000-0000-000000000002';
SET @demo_reviewer  = '30000000-0000-0000-0000-000000000003';
SET @demo_call      = '41000000-0000-0000-0000-000000000004';
SET @demo_aed       = '10000000-0000-0000-0000-000000000009';

UPDATE user_profiles
SET nickname = CASE id
  WHEN @demo_requester THEN '林晓'
  WHEN @demo_volunteer THEN '周朗'
  WHEN @demo_reviewer THEN '沈宁'
END,
phone = CASE id
  WHEN @demo_requester THEN '10000000001'
  WHEN @demo_volunteer THEN '10000000002'
  WHEN @demo_reviewer THEN '10000000003'
END
WHERE id IN (@demo_requester, @demo_volunteer, @demo_reviewer);

UPDATE rescue_calls
SET status = 'RESCUING',
    urgency = 'HIGH',
    address = '浙江图书馆一楼大厅',
    description = '读者突发晕厥，志愿者已到场，现场人员正在按急救指引协同处置。',
    matched_device_id = @demo_aed,
    matched_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 18 MINUTE),
    matched_distance_meters = 96,
    estimated_arrival_seconds = 148,
    match_strategy = 'ETA_V2_FIXED_RETRIEVAL',
    requested_by_user_id = @demo_requester,
    responder_user_id = @demo_volunteer,
    accepted_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 16 MINUTE),
    arrived_at_aed_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 12 MINUTE),
    arrived_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 7 MINUTE),
    rescue_started_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 MINUTE),
    aed_custody_status = 'AT_SCENE',
    responder_latitude = latitude,
    responder_longitude = longitude,
    responder_location_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 MINUTE),
    updated_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 MINUTE),
    event_sequence = 5
WHERE id = @demo_call;

UPDATE emergency_devices
SET status = 'RESERVED',
    owner_phone = '10000000120',
    registered_by_user_id = @demo_reviewer,
    reserved_for_call_id = @demo_call,
    reserved_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 18 MINUTE),
    version = version + 1
WHERE id = @demo_aed;

DELETE FROM rescue_events WHERE rescue_call_id = @demo_call;
INSERT INTO rescue_events (id, rescue_call_id, sequence_no, type, actor_user_id, summary, created_at) VALUES
  ('42000000-0000-0000-0000-000000000041', @demo_call, 1, 'CREATED', @demo_requester, '呼救已发出', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 22 MINUTE)),
  ('42000000-0000-0000-0000-000000000042', @demo_call, 2, 'AED_ASSIGNED', NULL, '已匹配浙江图书馆大厅 AED', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 18 MINUTE)),
  ('42000000-0000-0000-0000-000000000043', @demo_call, 3, 'ACCEPTED', @demo_volunteer, '志愿者已确认响应', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 16 MINUTE)),
  ('42000000-0000-0000-0000-000000000044', @demo_call, 4, 'ARRIVED_AT_REQUESTER', @demo_volunteer, '志愿者与 AED 已到达现场', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 7 MINUTE)),
  ('42000000-0000-0000-0000-000000000045', @demo_call, 5, 'RESCUE_STARTED', @demo_volunteer, '现场救援已开始', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 MINUTE));

INSERT INTO responder_presence (user_id, latitude, longitude, available, updated_at)
VALUES (@demo_volunteer, 30.2705, 120.1452, TRUE, UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE latitude = VALUES(latitude), longitude = VALUES(longitude), available = TRUE, updated_at = VALUES(updated_at);
