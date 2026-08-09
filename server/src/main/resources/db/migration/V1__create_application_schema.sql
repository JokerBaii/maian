
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `emergency_contacts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emergency_contacts` (
  `id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `name` varchar(60) NOT NULL,
  `phone` varchar(30) NOT NULL,
  `relation_name` varchar(30) NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_contact_user_created` (`user_id`,`created_at`),
  CONSTRAINT `fk_contact_user` FOREIGN KEY (`user_id`) REFERENCES `user_profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `emergency_device_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emergency_device_images` (
  `device_id` char(36) NOT NULL,
  `position` int NOT NULL,
  `image_url` varchar(500) NOT NULL,
  PRIMARY KEY (`device_id`,`position`),
  CONSTRAINT `fk_device_images_device` FOREIGN KEY (`device_id`) REFERENCES `emergency_devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `emergency_device_vehicle_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emergency_device_vehicle_images` (
  `device_id` char(36) NOT NULL,
  `position` int NOT NULL,
  `image_url` varchar(500) NOT NULL,
  PRIMARY KEY (`device_id`,`position`),
  CONSTRAINT `fk_device_vehicle_images_device` FOREIGN KEY (`device_id`) REFERENCES `emergency_devices` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `emergency_devices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emergency_devices` (
  `id` char(36) NOT NULL,
  `type` varchar(20) NOT NULL,
  `category` varchar(30) NOT NULL,
  `name` varchar(120) NOT NULL,
  `address` varchar(255) NOT NULL,
  `longitude` double NOT NULL,
  `latitude` double NOT NULL,
  `status` enum('PENDING_REVIEW','AVAILABLE','RESERVED','MAINTENANCE','OFFLINE','EXPIRED','REJECTED') NOT NULL,
  `owner_phone` varchar(40) DEFAULT NULL,
  `service_time` varchar(80) DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL,
  `expire_date` date DEFAULT NULL,
  `owner` varchar(120) DEFAULT NULL,
  `vehicle_info` varchar(120) DEFAULT NULL,
  `service_range` int DEFAULT NULL,
  `instructions` varchar(500) DEFAULT NULL,
  `last_location_at` timestamp(6) NULL DEFAULT NULL,
  `reserved_for_call_id` char(36) DEFAULT NULL,
  `reserved_at` timestamp(6) NULL DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `registered_by_user_id` char(36) NOT NULL,
  `review_note` varchar(300) DEFAULT NULL,
  `reviewed_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_active_rescue` (`reserved_for_call_id`),
  KEY `idx_device_type_status` (`type`,`status`),
  KEY `idx_device_location` (`latitude`,`longitude`),
  KEY `idx_device_dispatch` (`category`,`status`,`latitude`,`longitude`),
  KEY `idx_mobile_location_freshness` (`type`,`status`,`last_location_at`),
  KEY `idx_device_expiry_dispatch` (`status`,`expire_date`),
  KEY `idx_device_registered_user` (`registered_by_user_id`,`type`,`created_at`),
  CONSTRAINT `fk_device_registered_user` FOREIGN KEY (`registered_by_user_id`) REFERENCES `user_profiles` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `health_report_indicators`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `health_report_indicators` (
  `report_id` char(36) NOT NULL,
  `position` int NOT NULL,
  `name` varchar(100) NOT NULL,
  `value_text` varchar(100) NOT NULL,
  `unit` varchar(50) DEFAULT NULL,
  `reference_range` varchar(100) DEFAULT NULL,
  `abnormal` BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (`report_id`,`position`),
  CONSTRAINT `fk_health_indicator_report` FOREIGN KEY (`report_id`) REFERENCES `health_reports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `health_report_recommendations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `health_report_recommendations` (
  `report_id` char(36) NOT NULL,
  `position` int NOT NULL,
  `recommendation` varchar(500) NOT NULL,
  PRIMARY KEY (`report_id`,`position`),
  CONSTRAINT `fk_health_recommendation_report` FOREIGN KEY (`report_id`) REFERENCES `health_reports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `health_reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `health_reports` (
  `id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `checkup_date` date NOT NULL,
  `hospital` varchar(120) NOT NULL,
  `source_image_url` varchar(500) DEFAULT NULL,
  `risk_level` varchar(20) NOT NULL,
  `summary` varchar(1000) NOT NULL,
  `disclaimer` varchar(1000) NOT NULL,
  `analysis_source` varchar(30) NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_health_report_user_date` (`user_id`,`checkup_date`),
  CONSTRAINT `fk_health_report_user` FOREIGN KEY (`user_id`) REFERENCES `user_profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `heart_rate_readings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `heart_rate_readings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` char(36) NOT NULL,
  `wearable_id` char(36) DEFAULT NULL,
  `bpm` int NOT NULL,
  `scene` varchar(20) NOT NULL,
  `recorded_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_heart_rate_user_time` (`user_id`,`recorded_at` DESC),
  KEY `fk_heart_rate_wearable` (`wearable_id`),
  CONSTRAINT `fk_heart_rate_user` FOREIGN KEY (`user_id`) REFERENCES `user_profiles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_heart_rate_wearable` FOREIGN KEY (`wearable_id`) REFERENCES `wearable_devices` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `rescue_call_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rescue_call_images` (
  `rescue_call_id` char(36) NOT NULL,
  `position` int NOT NULL,
  `image_url` varchar(500) NOT NULL,
  PRIMARY KEY (`rescue_call_id`,`position`),
  CONSTRAINT `fk_rescue_images_call` FOREIGN KEY (`rescue_call_id`) REFERENCES `rescue_calls` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `rescue_call_symptoms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rescue_call_symptoms` (
  `rescue_call_id` char(36) NOT NULL,
  `symptom` varchar(50) NOT NULL,
  PRIMARY KEY (`rescue_call_id`,`symptom`),
  CONSTRAINT `fk_rescue_symptoms_call` FOREIGN KEY (`rescue_call_id`) REFERENCES `rescue_calls` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `rescue_calls`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rescue_calls` (
  `id` char(36) NOT NULL,
  `urgency` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  `address` varchar(255) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  `matched_device_id` char(36) DEFAULT NULL,
  `matched_at` timestamp(6) NULL DEFAULT NULL,
  `matched_distance_meters` int DEFAULT NULL,
  `estimated_arrival_seconds` int DEFAULT NULL,
  `match_strategy` varchar(40) DEFAULT NULL,
  `client_request_id` varchar(64) DEFAULT NULL,
  `requested_by_user_id` char(36) NOT NULL,
  `responder_user_id` char(36) DEFAULT NULL,
  `accepted_at` timestamp(6) NULL DEFAULT NULL,
  `completed_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rescue_client_request` (`client_request_id`),
  KEY `idx_rescue_status_created` (`status`,`created_at`),
  KEY `idx_rescue_location` (`latitude`,`longitude`),
  KEY `idx_rescue_matched_device` (`matched_device_id`),
  KEY `idx_rescue_requested_user` (`requested_by_user_id`,`created_at`),
  KEY `idx_rescue_responder_status` (`responder_user_id`,`status`),
  CONSTRAINT `fk_rescue_matched_device` FOREIGN KEY (`matched_device_id`) REFERENCES `emergency_devices` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_rescue_requested_user` FOREIGN KEY (`requested_by_user_id`) REFERENCES `user_profiles` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_rescue_responder_user` FOREIGN KEY (`responder_user_id`) REFERENCES `user_profiles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `science_article_interactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `science_article_interactions` (
  `id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `article_id` varchar(40) NOT NULL,
  `liked` BOOLEAN NOT NULL DEFAULT FALSE,
  `collected` BOOLEAN NOT NULL DEFAULT FALSE,
  `updated_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_science_interaction_user_article` (`user_id`,`article_id`),
  KEY `idx_science_interaction_article` (`article_id`),
  CONSTRAINT `fk_science_interaction_user` FOREIGN KEY (`user_id`) REFERENCES `user_profiles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `science_submissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `science_submissions` (
  `id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `title` varchar(50) NOT NULL,
  `category` varchar(30) NOT NULL,
  `content` varchar(2000) NOT NULL,
  `has_cover_image` BOOLEAN NOT NULL DEFAULT FALSE,
  `cover_image_url` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `review_note` varchar(300) DEFAULT NULL,
  `reviewed_at` timestamp(6) NULL DEFAULT NULL,
  `submitted_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_submission_user_time` (`user_id`,`submitted_at`),
  KEY `idx_submission_status_time` (`status`,`submitted_at`),
  CONSTRAINT `fk_submission_user` FOREIGN KEY (`user_id`) REFERENCES `user_profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_profiles` (
  `id` char(36) NOT NULL,
  `nickname` varchar(60) NOT NULL,
  `phone` varchar(30) NOT NULL,
  `role` varchar(30) NOT NULL,
  `real_name` varchar(60) DEFAULT NULL,
  `id_card_masked` varchar(30) DEFAULT NULL,
  `verified` BOOLEAN NOT NULL DEFAULT FALSE,
  `created_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_settings` (
  `user_id` char(36) NOT NULL,
  `rescue_push` BOOLEAN NOT NULL DEFAULT TRUE,
  `health_alert` BOOLEAN NOT NULL DEFAULT TRUE,
  `science_update` BOOLEAN NOT NULL DEFAULT FALSE,
  `location_share` BOOLEAN NOT NULL DEFAULT TRUE,
  `health_data_share` BOOLEAN NOT NULL DEFAULT FALSE,
  `max_heart_rate` int NOT NULL DEFAULT '120',
  `min_heart_rate` int NOT NULL DEFAULT '50',
  `updated_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_settings_user` FOREIGN KEY (`user_id`) REFERENCES `user_profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `wearable_devices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wearable_devices` (
  `id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `device_identifier` varchar(160) NOT NULL,
  `name` varchar(120) NOT NULL,
  `type` varchar(40) NOT NULL,
  `connected` BOOLEAN NOT NULL DEFAULT FALSE,
  `battery` int DEFAULT NULL,
  `last_seen_at` timestamp(6) NULL DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wearable_user` (`user_id`),
  UNIQUE KEY `uk_wearable_identifier` (`device_identifier`),
  CONSTRAINT `fk_wearable_user` FOREIGN KEY (`user_id`) REFERENCES `user_profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
