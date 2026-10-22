CREATE TABLE assistant_reminders (
  id CHAR(36) NOT NULL PRIMARY KEY,
  owner_id CHAR(36) NOT NULL,
  title VARCHAR(120) NOT NULL,
  kind VARCHAR(24) NOT NULL,
  target_id CHAR(36) NULL,
  status VARCHAR(20) NOT NULL,
  result VARCHAR(300) NULL,
  due_at DATETIME(6) NULL,
  next_check_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  triggered_at DATETIME(6) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  KEY idx_reminder_owner (owner_id, status),
  KEY idx_reminder_check (status, next_check_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
