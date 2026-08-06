-- 救援完成后呼救方对志愿者的评价。
-- 一条呼救最多一条评价，由呼救方在救援完成后提交。

CREATE TABLE `rescue_feedback` (
  `id` char(36) NOT NULL,
  `rescue_call_id` char(36) NOT NULL,
  `from_user_id` char(36) NOT NULL,
  `to_user_id` char(36) DEFAULT NULL,
  `rating` int NOT NULL,
  `comment` varchar(500) DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_feedback_rescue` (`rescue_call_id`),
  KEY `idx_feedback_to_user` (`to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
