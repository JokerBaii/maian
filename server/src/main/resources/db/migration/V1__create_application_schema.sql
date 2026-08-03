
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


-- Competition showcase identities. These accounts are explicitly labelled in the app
-- and contain no claimed phone number, real-name verification or identity document.
INSERT INTO user_profiles (
  id, nickname, phone, role, real_name, id_card_masked, verified, created_at
) VALUES
  ('30000000-0000-0000-0000-000000000001', '体验用户', '未绑定', 'USER', NULL, NULL, FALSE, UTC_TIMESTAMP(6)),
  ('30000000-0000-0000-0000-000000000002', '志愿者体验账号', '未绑定', 'VOLUNTEER', NULL, NULL, FALSE, UTC_TIMESTAMP(6)),
  ('30000000-0000-0000-0000-000000000003', '审核体验账号', '未绑定', 'ADMIN', NULL, NULL, FALSE, UTC_TIMESTAMP(6));

-- Competition showcase fixtures. They make the cross-role workflow immediately
-- demonstrable and should be replaced by reviewed field data for real operation.
INSERT INTO emergency_devices (
  id, type, category, name, address, longitude, latitude, status,
  owner_phone, service_time, created_at, expire_date, owner, vehicle_info,
  service_range, instructions, last_location_at, version,
  registered_by_user_id, review_note, reviewed_at
) VALUES
  ('10000000-0000-0000-0000-000000000001', 'FIXED', 'AED', '紫金港体育中心 AED', '余杭塘路体育中心一层服务台', 120.0869, 30.3055, 'AVAILABLE', NULL, '06:00-22:00', UTC_TIMESTAMP(6), '2028-12-31', '场馆运营中心', NULL, NULL, '位于一层服务台右侧设备柜', NULL, 0, '30000000-0000-0000-0000-000000000001', '资料完整，准予共享', UTC_TIMESTAMP(6)),
  ('10000000-0000-0000-0000-000000000002', 'FIXED', 'AED', '西湖文化广场 AED', '西湖文化广场地下一层服务中心', 120.1633, 30.2794, 'AVAILABLE', NULL, '全天', UTC_TIMESTAMP(6), '2029-06-30', '广场服务中心', NULL, NULL, '服务中心入口处红色设备柜', NULL, 0, '30000000-0000-0000-0000-000000000001', '资料完整，准予共享', UTC_TIMESTAMP(6)),
  ('10000000-0000-0000-0000-000000000003', 'FIXED', '急救箱', '城西商业中心急救箱', '丰潭路商业中心一层总服务台', 120.1198, 30.2942, 'AVAILABLE', NULL, '10:00-22:00', UTC_TIMESTAMP(6), '2028-09-30', '商业中心服务台', NULL, NULL, '向总服务台工作人员领取', NULL, 0, '30000000-0000-0000-0000-000000000001', '资料完整，准予共享', UTC_TIMESTAMP(6)),
  ('10000000-0000-0000-0000-000000000004', 'FIXED', 'AED', '东站候车大厅 AED', '火车东站候车大厅B区服务台', 120.2133, 30.2907, 'MAINTENANCE', NULL, '全天', UTC_TIMESTAMP(6), '2028-10-31', '交通枢纽服务中心', NULL, NULL, '设备正在维护，请选择其他点位', NULL, 0, '30000000-0000-0000-0000-000000000003', '维护状态已确认', UTC_TIMESTAMP(6)),
  ('10000000-0000-0000-0000-000000000005', 'FIXED', '急救包', '市民服务中心急救包', '新业路市民服务中心L楼大厅', 120.2128, 30.2547, 'AVAILABLE', NULL, '08:30-17:30', UTC_TIMESTAMP(6), '2028-11-30', '市民服务中心', NULL, NULL, '大厅志愿服务台领取', NULL, 0, '30000000-0000-0000-0000-000000000003', '资料完整，准予共享', UTC_TIMESTAMP(6)),
  ('20000000-0000-0000-0000-000000000001', 'MOBILE', 'AED', '西湖区流动 AED', '当前位于文一路附近', 120.1300, 30.2700, 'AVAILABLE', NULL, '全天', UTC_TIMESTAMP(6), '2029-03-31', '城市救援志愿服务队', '白色新能源轿车', 5, '确认调度后由志愿者送达', UTC_TIMESTAMP(6), 0, '30000000-0000-0000-0000-000000000002', '设备与志愿者资料已核验', UTC_TIMESTAMP(6)),
  ('20000000-0000-0000-0000-000000000002', 'MOBILE', '急救包', '拱墅区流动急救包', '当前位于莫干山路附近', 120.1500, 30.3200, 'AVAILABLE', NULL, '工作日 08:00-18:00', UTC_TIMESTAMP(6), '2028-08-31', '城市救援志愿服务队', '蓝色紧凑型轿车', 5, '确认调度后由志愿者送达', UTC_TIMESTAMP(6), 0, '30000000-0000-0000-0000-000000000002', '设备与志愿者资料已核验', UTC_TIMESTAMP(6)),
  ('20000000-0000-0000-0000-000000000003', 'MOBILE', 'AED', '上城区流动 AED', '当前位置暂不可用', 120.1900, 30.2400, 'OFFLINE', NULL, '工作日 09:00-18:00', UTC_TIMESTAMP(6), '2029-01-31', '城市救援志愿服务队', '银色小型SUV', 5, '恢复在线后可参与调度', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 HOUR), 0, '30000000-0000-0000-0000-000000000002', '设备与志愿者资料已核验', UTC_TIMESTAMP(6)),
  ('10000000-0000-0000-0000-000000000006', 'FIXED', 'AED', '黄龙体育馆东门 AED', '黄龙路体育馆东门安保服务台', 120.1438, 30.2728, 'AVAILABLE', NULL, '06:30-22:00', UTC_TIMESTAMP(6), '2029-08-31', '黄龙场馆服务中心', NULL, NULL, '东门安保台内侧设备柜', NULL, 0, '30000000-0000-0000-0000-000000000001', '资料完整，准予共享', UTC_TIMESTAMP(6)),
  ('10000000-0000-0000-0000-000000000007', 'FIXED', 'AED', '武林广场地铁站 AED', '武林广场地铁站B口客服中心', 120.1651, 30.2750, 'AVAILABLE', NULL, '06:00-23:30', UTC_TIMESTAMP(6), '2029-03-31', '地铁站务中心', NULL, NULL, 'B口客服中心背侧设备柜', NULL, 0, '30000000-0000-0000-0000-000000000001', '资料完整，准予共享', UTC_TIMESTAMP(6)),
  ('10000000-0000-0000-0000-000000000008', 'FIXED', '急救箱', '湖滨步行街急救箱', '延安路湖滨步行街游客服务中心', 120.1687, 30.2567, 'AVAILABLE', NULL, '09:00-22:30', UTC_TIMESTAMP(6), '2028-12-31', '湖滨游客服务中心', NULL, NULL, '游客服务中心前台领取', NULL, 0, '30000000-0000-0000-0000-000000000001', '资料完整，准予共享', UTC_TIMESTAMP(6)),
  ('10000000-0000-0000-0000-000000000009', 'FIXED', 'AED', '浙江图书馆大厅 AED', '曙光路浙江图书馆一楼咨询台', 120.1449, 30.2701, 'AVAILABLE', NULL, '09:00-20:30', UTC_TIMESTAMP(6), '2029-05-31', '图书馆物业服务中心', NULL, NULL, '一楼总咨询台西侧', NULL, 0, '30000000-0000-0000-0000-000000000001', '资料完整，准予共享', UTC_TIMESTAMP(6)),
  ('10000000-0000-0000-0000-000000000010', 'FIXED', 'AED', '钱江新城城市阳台 AED', '城市阳台游客服务站内', 120.2215, 30.2464, 'AVAILABLE', NULL, '08:00-22:00', UTC_TIMESTAMP(6), '2029-11-30', '城市阳台运营中心', NULL, NULL, '游客服务站入口右侧', NULL, 0, '30000000-0000-0000-0000-000000000003', '资料完整，准予共享', UTC_TIMESTAMP(6)),
  ('10000000-0000-0000-0000-000000000011', 'FIXED', '急救包', '运河广场便民急救包', '桥弄街运河广场游客中心', 120.1410, 30.3202, 'AVAILABLE', NULL, '09:00-21:00', UTC_TIMESTAMP(6), '2028-10-31', '运河景区服务中心', NULL, NULL, '游客中心值班台领取', NULL, 0, '30000000-0000-0000-0000-000000000003', '资料完整，准予共享', UTC_TIMESTAMP(6)),
  ('10000000-0000-0000-0000-000000000012', 'FIXED', 'AED', '杭州大剧院 AED', '新业路杭州大剧院东侧服务台', 120.2187, 30.2486, 'AVAILABLE', NULL, '09:00-22:30', UTC_TIMESTAMP(6), '2029-09-30', '剧院场务中心', NULL, NULL, '东侧服务台后方设备柜', NULL, 0, '30000000-0000-0000-0000-000000000003', '资料完整，准予共享', UTC_TIMESTAMP(6)),
  ('20000000-0000-0000-0000-000000000004', 'MOBILE', 'AED', '滨江区流动 AED', '当前位于江南大道附近', 120.1908, 30.2083, 'AVAILABLE', NULL, '07:00-22:00', UTC_TIMESTAMP(6), '2029-06-30', '滨江应急志愿服务队', '白色七座商务车', 8, '接单后沿最快路线送达', UTC_TIMESTAMP(6), 0, '30000000-0000-0000-0000-000000000002', '设备与志愿者资料已核验', UTC_TIMESTAMP(6)),
  ('20000000-0000-0000-0000-000000000005', 'MOBILE', 'AED', '城北流动 AED', '当前位于大关路附近', 120.1519, 30.3066, 'AVAILABLE', NULL, '08:00-21:00', UTC_TIMESTAMP(6), '2029-04-30', '拱墅应急志愿服务队', '蓝色新能源SUV', 7, '接单后电话确认交接点', UTC_TIMESTAMP(6), 0, '30000000-0000-0000-0000-000000000002', '设备与志愿者资料已核验', UTC_TIMESTAMP(6)),
  ('20000000-0000-0000-0000-000000000006', 'MOBILE', '急救包', '城西流动急救包', '当前位于古墩路附近', 120.1054, 30.2895, 'AVAILABLE', NULL, '工作日 07:30-20:00', UTC_TIMESTAMP(6), '2028-12-31', '西湖应急志愿服务队', '灰色紧凑型轿车', 6, '接单后由志愿者送达', UTC_TIMESTAMP(6), 0, '30000000-0000-0000-0000-000000000002', '设备与志愿者资料已核验', UTC_TIMESTAMP(6)),
  ('20000000-0000-0000-0000-000000000007', 'MOBILE', 'AED', '城东流动 AED', '当前位于艮山西路附近', 120.1995, 30.2812, 'AVAILABLE', NULL, '08:00-22:00', UTC_TIMESTAMP(6), '2029-07-31', '上城应急志愿服务队', '红白标识小型SUV', 7, '接单后由志愿者送达', UTC_TIMESTAMP(6), 0, '30000000-0000-0000-0000-000000000002', '设备与志愿者资料已核验', UTC_TIMESTAMP(6)),
  ('90000000-0000-0000-0000-000000000001', 'FIXED', 'AED', '社区服务站 AED', '文新街道社区服务站一层大厅', 120.1124, 30.2878, 'PENDING_REVIEW', NULL, '08:00-20:00', UTC_TIMESTAMP(6), '2029-05-31', '社区服务站', NULL, NULL, '入口左侧急救设备柜', NULL, 0, '30000000-0000-0000-0000-000000000001', NULL, NULL);

INSERT INTO rescue_calls (
  id, urgency, status, latitude, longitude, address, description,
  created_at, updated_at, matched_device_id, matched_at,
  matched_distance_meters, estimated_arrival_seconds, match_strategy,
  client_request_id, requested_by_user_id, responder_user_id,
  accepted_at, completed_at
) VALUES
  ('40000000-0000-0000-0000-000000000001', 'CRITICAL', 'COMPLETED', 30.2791, 120.1640,
   '西湖文化广场东侧入口', '路人突发晕厥，现场完成意识判断并持续陪护。',
   DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY),
   '10000000-0000-0000-0000-000000000002', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY),
   120, 184, 'ETA_V2_FIXED_RETRIEVAL', 'seed-history-001',
   '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002',
   DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY)),
  ('40000000-0000-0000-0000-000000000002', 'HIGH', 'CANCELLED', 30.2725, 120.1441,
   '黄龙体育馆东门外', '运动后出现头晕，休息后症状缓解并主动取消呼救。',
   DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 8 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 8 DAY),
   NULL, NULL, NULL, NULL, NULL, 'seed-history-002',
   '30000000-0000-0000-0000-000000000001', NULL, NULL, NULL),
  ('40000000-0000-0000-0000-000000000003', 'HIGH', 'COMPLETED', 30.3051, 120.0874,
   '紫金港体育中心南侧通道', '现场发生外伤出血，志愿者完成压迫止血并协助转交急救人员。',
   DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 14 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 14 DAY),
   '10000000-0000-0000-0000-000000000001', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 14 DAY),
   210, 265, 'ETA_V2_FIXED_RETRIEVAL', 'seed-history-003',
   '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002',
   DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 14 DAY), DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 14 DAY));

INSERT INTO rescue_call_symptoms (rescue_call_id, symptom) VALUES
  ('40000000-0000-0000-0000-000000000001', '意识不清'),
  ('40000000-0000-0000-0000-000000000002', '头晕不适'),
  ('40000000-0000-0000-0000-000000000003', '外伤出血');

INSERT INTO emergency_contacts (
  id, user_id, name, phone, relation_name, created_at
) VALUES
  ('31000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '李女士', '13800000000', '家属', UTC_TIMESTAMP(6)),
  ('31000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', '王先生', '13900000000', '朋友', UTC_TIMESTAMP(6)),
  ('31000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000001', '社区值班室', '057188001120', '社区服务', UTC_TIMESTAMP(6)),
  ('31000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000001', '赵医生', '13700000000', '家庭医生', UTC_TIMESTAMP(6));

INSERT INTO user_settings (
  user_id, rescue_push, health_alert, science_update,
  location_share, health_data_share, max_heart_rate, min_heart_rate, updated_at
) VALUES
  ('30000000-0000-0000-0000-000000000001', TRUE, TRUE, TRUE, TRUE, FALSE, 120, 50, UTC_TIMESTAMP(6)),
  ('30000000-0000-0000-0000-000000000002', TRUE, TRUE, TRUE, TRUE, FALSE, 120, 50, UTC_TIMESTAMP(6)),
  ('30000000-0000-0000-0000-000000000003', TRUE, TRUE, TRUE, TRUE, FALSE, 120, 50, UTC_TIMESTAMP(6));

INSERT INTO wearable_devices (
  id, user_id, device_identifier, name, type, connected, battery,
  last_seen_at, created_at, updated_at
) VALUES
  ('70000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
   'pulsecare-watch-s1', 'PulseCare Watch S1', 'watch', TRUE, 86,
   UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  ('70000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002',
   'pulsecare-band-v2', 'PulseCare Band V2', 'band', TRUE, 72,
   UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  ('70000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003',
   'pulsecare-watch-pro', 'PulseCare Watch Pro', 'watch', TRUE, 91,
   UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO heart_rate_readings (
  user_id, wearable_id, bpm, scene, recorded_at
) VALUES
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 68, 'resting', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 DAY)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 74, 'daily', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 DAY)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 82, 'walking', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 DAY)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 71, 'resting', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 DAY)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 88, 'walking', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 DAY)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 76, 'daily', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 72, 'resting', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 90 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 79, 'daily', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 60 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 126, 'exercise', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 30 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 84, 'recovery', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 15 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 70, 'resting', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 12 HOUR)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 73, 'daily', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 10 HOUR)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 78, 'walking', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 8 HOUR)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 81, 'daily', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 HOUR)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 75, 'resting', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 5 HOUR)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 92, 'walking', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 4 HOUR)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 87, 'daily', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 3 HOUR)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 80, 'recovery', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 HOUR)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 69, 'sleeping', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 630 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 71, 'resting', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 570 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 74, 'resting', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 510 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 77, 'daily', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 450 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 83, 'walking', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 390 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 79, 'daily', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 330 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 86, 'walking', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 270 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 82, 'daily', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 210 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 96, 'walking', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 150 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 90, 'recovery', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 105 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 81, 'daily', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 45 MINUTE)),
  ('30000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 76, 'resting', DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 2 MINUTE));

INSERT INTO heart_rate_readings (user_id, wearable_id, bpm, scene, recorded_at)
SELECT '30000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', bpm, scene, recorded_at
FROM heart_rate_readings
WHERE user_id = '30000000-0000-0000-0000-000000000001';

INSERT INTO heart_rate_readings (user_id, wearable_id, bpm, scene, recorded_at)
SELECT '30000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000003', bpm, scene, recorded_at
FROM heart_rate_readings
WHERE user_id = '30000000-0000-0000-0000-000000000001';

INSERT INTO health_reports (
  id, user_id, checkup_date, hospital, source_image_url,
  risk_level, summary, disclaimer, analysis_source, created_at
) VALUES
  ('60000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
   DATE_SUB(UTC_DATE(), INTERVAL 30 DAY), '城西健康管理中心', NULL,
   'MEDIUM', '本次报告发现 2 项指标超出参考范围，建议结合既往情况复核。',
   '本结果仅用于健康信息整理，不能替代医生诊断；如有胸痛、呼吸困难或意识异常，请立即呼叫急救。',
   'RULE_BASED', UTC_TIMESTAMP(6));

INSERT INTO health_report_indicators (
  report_id, position, name, value_text, unit, reference_range, abnormal
) VALUES
  ('60000000-0000-0000-0000-000000000001', 0, '血红蛋白', '128', 'g/L', '130-175', TRUE),
  ('60000000-0000-0000-0000-000000000001', 1, '总胆固醇', '5.8', 'mmol/L', '2.8-5.2', TRUE),
  ('60000000-0000-0000-0000-000000000001', 2, '空腹血糖', '5.2', 'mmol/L', '3.9-6.1', FALSE),
  ('60000000-0000-0000-0000-000000000001', 3, '血小板计数', '220', '×10⁹/L', '125-350', FALSE);

INSERT INTO health_report_recommendations (
  report_id, position, recommendation
) VALUES
  ('60000000-0000-0000-0000-000000000001', 0, '携带完整报告咨询医生，结合既往情况复核异常指标'),
  ('60000000-0000-0000-0000-000000000001', 1, '保持规律作息和均衡饮食，并按医生建议复查');

INSERT INTO health_reports (
  id, user_id, checkup_date, hospital, source_image_url,
  risk_level, summary, disclaimer, analysis_source, created_at
) VALUES
  ('60000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', DATE_SUB(UTC_DATE(), INTERVAL 30 DAY), '城西健康管理中心', NULL, 'MEDIUM', '本次报告发现 2 项指标超出参考范围，建议结合既往情况复核。', '本结果仅用于健康信息整理，不能替代医生诊断；如有胸痛、呼吸困难或意识异常，请立即呼叫急救。', 'RULE_BASED', UTC_TIMESTAMP(6)),
  ('60000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', DATE_SUB(UTC_DATE(), INTERVAL 30 DAY), '城西健康管理中心', NULL, 'MEDIUM', '本次报告发现 2 项指标超出参考范围，建议结合既往情况复核。', '本结果仅用于健康信息整理，不能替代医生诊断；如有胸痛、呼吸困难或意识异常，请立即呼叫急救。', 'RULE_BASED', UTC_TIMESTAMP(6));

INSERT INTO health_report_indicators (report_id, position, name, value_text, unit, reference_range, abnormal)
SELECT '60000000-0000-0000-0000-000000000002', position, name, value_text, unit, reference_range, abnormal
FROM health_report_indicators
WHERE report_id = '60000000-0000-0000-0000-000000000001';

INSERT INTO health_report_indicators (report_id, position, name, value_text, unit, reference_range, abnormal)
SELECT '60000000-0000-0000-0000-000000000003', position, name, value_text, unit, reference_range, abnormal
FROM health_report_indicators
WHERE report_id = '60000000-0000-0000-0000-000000000001';

INSERT INTO health_report_recommendations (report_id, position, recommendation)
SELECT '60000000-0000-0000-0000-000000000002', position, recommendation
FROM health_report_recommendations
WHERE report_id = '60000000-0000-0000-0000-000000000001';

INSERT INTO health_report_recommendations (report_id, position, recommendation)
SELECT '60000000-0000-0000-0000-000000000003', position, recommendation
FROM health_report_recommendations
WHERE report_id = '60000000-0000-0000-0000-000000000001';

INSERT INTO science_submissions (
  id, user_id, title, category, content, has_cover_image,
  cover_image_url, status, review_note, reviewed_at, submitted_at
) VALUES
  ('80000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
   '社区 AED 日常检查要点', 'device',
   '建议定期检查设备外观、电极片有效期、电池状态和标识完整性，并及时记录维护结果。',
   FALSE, NULL, 'PENDING', NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 DAY)),
  ('80000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001',
   '公共场所发现有人晕倒的处置顺序', 'emergency',
   '先确认现场安全，轻拍呼唤判断意识，立即呼叫 120 并请周围人员取来 AED；无正常呼吸时开始胸外按压。',
   FALSE, NULL, 'PENDING', NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 18 HOUR)),
  ('80000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000002',
   '运动后心率恢复观察要点', 'health',
   '运动结束后逐步降低强度并观察心率恢复速度；出现胸痛、明显气促或头晕时停止活动并及时求助。',
   FALSE, NULL, 'PENDING', NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 12 HOUR)),
  ('80000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000001',
   '家庭急救包每月检查清单', 'device',
   '核对敷料、手套、消毒用品和应急联系方式，清理过期物品并保持急救包放置位置清晰易取。',
   FALSE, NULL, 'PENDING', NULL, NULL, DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 6 HOUR));


/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
