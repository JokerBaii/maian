CREATE TABLE coordination_tasks (
  id CHAR(36) NOT NULL PRIMARY KEY,
  owner_user_id CHAR(36) NOT NULL,
  device_id CHAR(36) NOT NULL,
  rescue_call_id CHAR(36) NULL,
  device_name VARCHAR(120) NOT NULL,
  trigger_type VARCHAR(40) NOT NULL,
  event_key VARCHAR(160) NOT NULL,
  status VARCHAR(24) NOT NULL,
  execution_mode VARCHAR(24) NOT NULL,
  summary VARCHAR(600) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  lease_until DATETIME(6) NULL,
  completed_at DATETIME(6) NULL,
  actions_applied BOOLEAN NOT NULL DEFAULT FALSE,
  attention_required BOOLEAN NOT NULL DEFAULT FALSE,
  version BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_coordination_event (event_key),
  KEY idx_coordination_owner (owner_user_id, created_at),
  KEY idx_coordination_rescue (rescue_call_id, created_at),
  KEY idx_coordination_pending (status, lease_until, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE coordination_steps (
  task_id CHAR(36) NOT NULL,
  position INT NOT NULL,
  module_name VARCHAR(40) NOT NULL,
  summary VARCHAR(600) NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  PRIMARY KEY (task_id, position),
  CONSTRAINT fk_coordination_step FOREIGN KEY (task_id) REFERENCES coordination_tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
