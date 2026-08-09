-- 演示事件的相对时间在部署时对齐，避免长期运行后仍显示为数日前的“进行中”。
UPDATE rescue_calls
SET created_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 22 MINUTE),
    updated_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 MINUTE)
WHERE id = '41000000-0000-0000-0000-000000000004'
  AND client_request_id = 'seed-open-004';

UPDATE active_rescue_locks
SET acquired_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 22 MINUTE)
WHERE rescue_call_id = '41000000-0000-0000-0000-000000000004';

