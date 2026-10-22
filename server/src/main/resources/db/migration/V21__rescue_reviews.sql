CREATE TABLE rescue_reviews (
  rescue_call_id CHAR(36) NOT NULL PRIMARY KEY,
  status VARCHAR(24) NOT NULL,
  execution_mode VARCHAR(24) NOT NULL,
  summary VARCHAR(600) NOT NULL,
  use_model BOOLEAN NOT NULL DEFAULT FALSE,
  source_sequence BIGINT NOT NULL DEFAULT 0,
  feedback_present BOOLEAN NOT NULL DEFAULT FALSE,
  result_json LONGTEXT NULL,
  execution_token CHAR(36) NULL,
  lease_until DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  completed_at DATETIME(6) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  KEY idx_rescue_review_pending (status, lease_until, updated_at),
  CONSTRAINT fk_rescue_review_call FOREIGN KEY (rescue_call_id) REFERENCES rescue_calls(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rescue_review_steps (
  rescue_call_id CHAR(36) NOT NULL,
  position INT NOT NULL,
  module_name VARCHAR(40) NOT NULL,
  summary VARCHAR(600) NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  PRIMARY KEY (rescue_call_id, position),
  CONSTRAINT fk_rescue_review_step FOREIGN KEY (rescue_call_id) REFERENCES rescue_reviews(rescue_call_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
