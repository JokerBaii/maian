ALTER TABLE emergency_devices
  MODIFY COLUMN status ENUM(
    'PENDING_REVIEW','AVAILABLE','RESERVED','MAINTENANCE','OFFLINE',
    'DISABLED','EXPIRED','REJECTED'
  ) NOT NULL;

UPDATE emergency_devices SET status = 'DISABLED' WHERE status IN ('MAINTENANCE', 'OFFLINE');

ALTER TABLE emergency_devices
  MODIFY COLUMN status ENUM(
    'PENDING_REVIEW','AVAILABLE','RESERVED','DISABLED','EXPIRED','REJECTED'
  ) NOT NULL;

-- A mobile device only becomes dispatchable after a real heartbeat arrives.
UPDATE emergency_devices SET last_location_at = NULL WHERE type = 'MOBILE';

ALTER TABLE rescue_calls
  MODIFY COLUMN status varchar(32) NOT NULL,
  ADD COLUMN match_deadline_at timestamp(6) NULL AFTER updated_at,
  ADD COLUMN aed_custody_status varchar(24) NULL AFTER match_strategy,
  ADD COLUMN arrived_at_aed_at timestamp(6) NULL AFTER accepted_at,
  ADD COLUMN arrived_at timestamp(6) NULL AFTER arrived_at_aed_at,
  ADD COLUMN rescue_started_at timestamp(6) NULL AFTER arrived_at,
  ADD COLUMN completion_submitted_at timestamp(6) NULL AFTER rescue_started_at,
  ADD COLUMN confirmation_deadline_at timestamp(6) NULL AFTER completion_submitted_at,
  ADD COLUMN aed_returned_at timestamp(6) NULL AFTER completed_at,
  ADD COLUMN responder_latitude double NULL AFTER aed_returned_at,
  ADD COLUMN responder_longitude double NULL AFTER responder_latitude,
  ADD COLUMN responder_location_at timestamp(6) NULL AFTER responder_longitude,
  ADD COLUMN event_sequence bigint NOT NULL DEFAULT 1 AFTER responder_location_at,
  ADD COLUMN version bigint NOT NULL DEFAULT 0 AFTER event_sequence;

UPDATE rescue_calls SET status = 'USER_CANCELLED' WHERE status = 'CANCELLED';
UPDATE rescue_calls SET status = 'EN_ROUTE_TO_REQUESTER' WHERE status = 'ACCEPTED';
UPDATE rescue_calls SET status = 'MATCHING' WHERE status = 'PENDING';
UPDATE rescue_calls
SET aed_custody_status = CASE
  WHEN status = 'COMPLETED' THEN 'RETURNED'
  WHEN matched_device_id IS NOT NULL THEN 'RESERVED'
  ELSE NULL
END;
UPDATE rescue_calls
SET match_deadline_at = DATE_ADD(created_at, INTERVAL 10 MINUTE)
WHERE status = 'MATCHING' AND match_deadline_at IS NULL;

ALTER TABLE rescue_calls
  DROP INDEX uk_rescue_client_request,
  ADD UNIQUE KEY uk_rescue_user_client_request (requested_by_user_id, client_request_id),
  ADD KEY idx_rescue_match_scheduler (status, updated_at, match_deadline_at),
  ADD KEY idx_rescue_confirmation_scheduler (status, confirmation_deadline_at);

CREATE TABLE active_rescue_locks (
  user_id char(36) NOT NULL,
  rescue_call_id char(36) NOT NULL,
  acquired_at timestamp(6) NOT NULL,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_active_rescue_call (rescue_call_id),
  CONSTRAINT fk_active_rescue_user FOREIGN KEY (user_id) REFERENCES user_profiles(id) ON DELETE CASCADE,
  CONSTRAINT fk_active_rescue_call FOREIGN KEY (rescue_call_id) REFERENCES rescue_calls(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Legacy versions allowed several active calls for one requester. Preserve the
-- most progressed/recent one and close the rest before enforcing the lock.
CREATE TEMPORARY TABLE active_rescue_winners (
  user_id char(36) NOT NULL PRIMARY KEY,
  rescue_call_id char(36) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO active_rescue_winners (user_id, rescue_call_id)
SELECT requested_by_user_id, id
FROM (
  SELECT
    requested_by_user_id,
    id,
    ROW_NUMBER() OVER (
      PARTITION BY requested_by_user_id
      ORDER BY
        CASE status
          WHEN 'PENDING_CONFIRMATION' THEN 1
          WHEN 'RESCUING' THEN 2
          WHEN 'ARRIVED' THEN 3
          WHEN 'EN_ROUTE_TO_REQUESTER' THEN 4
          WHEN 'EN_ROUTE_TO_AED' THEN 5
          WHEN 'MATCHING' THEN 6
          ELSE 7
        END,
        created_at DESC,
        id DESC
    ) AS active_rank
  FROM rescue_calls
  WHERE status IN (
    'PENDING','MATCHING','EN_ROUTE_TO_AED','EN_ROUTE_TO_REQUESTER',
    'ARRIVED','RESCUING','PENDING_CONFIRMATION'
  )
) ranked
WHERE active_rank = 1;

UPDATE rescue_calls call_record
LEFT JOIN active_rescue_winners winner
  ON winner.user_id = call_record.requested_by_user_id
  AND winner.rescue_call_id = call_record.id
SET call_record.status = 'SYSTEM_FAILED',
    call_record.updated_at = UTC_TIMESTAMP(6)
WHERE call_record.status IN (
    'PENDING','MATCHING','EN_ROUTE_TO_AED','EN_ROUTE_TO_REQUESTER',
    'ARRIVED','RESCUING','PENDING_CONFIRMATION'
  )
  AND winner.rescue_call_id IS NULL;

UPDATE emergency_devices device
JOIN rescue_calls call_record ON call_record.id = device.reserved_for_call_id
SET device.status = 'AVAILABLE',
    device.reserved_for_call_id = NULL,
    device.reserved_at = NULL
WHERE call_record.status = 'SYSTEM_FAILED';

INSERT INTO active_rescue_locks (user_id, rescue_call_id, acquired_at)
SELECT requested_by_user_id, id, created_at
FROM rescue_calls
WHERE status IN (
  'PENDING','MATCHING','EN_ROUTE_TO_AED','EN_ROUTE_TO_REQUESTER',
  'ARRIVED','RESCUING','PENDING_CONFIRMATION'
);

DROP TEMPORARY TABLE active_rescue_winners;

CREATE TABLE responder_presence (
  user_id char(36) NOT NULL,
  latitude double NOT NULL,
  longitude double NOT NULL,
  available boolean NOT NULL DEFAULT FALSE,
  updated_at timestamp(6) NOT NULL,
  PRIMARY KEY (user_id),
  KEY idx_responder_presence_availability (available, updated_at),
  CONSTRAINT fk_responder_presence_user FOREIGN KEY (user_id) REFERENCES user_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rescue_events (
  id char(36) NOT NULL,
  rescue_call_id char(36) NOT NULL,
  sequence_no bigint NOT NULL,
  type varchar(40) NOT NULL,
  actor_user_id char(36) NULL,
  summary varchar(300) NULL,
  created_at timestamp(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rescue_event_sequence (rescue_call_id, sequence_no),
  KEY idx_rescue_event_call_time (rescue_call_id, created_at),
  CONSTRAINT fk_rescue_event_call FOREIGN KEY (rescue_call_id) REFERENCES rescue_calls(id) ON DELETE CASCADE,
  CONSTRAINT fk_rescue_event_actor FOREIGN KEY (actor_user_id) REFERENCES user_profiles(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
