CREATE TABLE user_credentials (
  user_id char(36) NOT NULL,
  phone varchar(30) NOT NULL,
  password_hash varchar(100) NOT NULL,
  enabled boolean NOT NULL DEFAULT TRUE,
  created_at timestamp(6) NOT NULL,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_user_credential_phone (phone),
  CONSTRAINT fk_user_credential_profile FOREIGN KEY (user_id) REFERENCES user_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
