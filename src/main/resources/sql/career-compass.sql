-- career-compass schema (Plan B: session-centric model)
-- MySQL 8.0+
--
-- CREATE DATABASE IF NOT EXISTS career_compass DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE career_compass;
CREATE DATABASE career_compass DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE career_compass;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `flow_event`;
DROP TABLE IF EXISTS `chat_message`;
DROP TABLE IF EXISTS `assessment_artifact`;
DROP TABLE IF EXISTS `assessment_outcome`;
DROP TABLE IF EXISTS `assessment_session`;
DROP TABLE IF EXISTS `assessment_participant`;

-- Legacy tables (removed in Plan B; drop if migrating from old schema)
DROP TABLE IF EXISTS `all_chat`;
DROP TABLE IF EXISTS `feedback`;
DROP TABLE IF EXISTS `skill_test`;
DROP TABLE IF EXISTS `career_test`;
DROP TABLE IF EXISTS `chat_end_result`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `assessment_participant` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '测评参与者 ID',
  `name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '姓名',
  `work_years` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作年限档位：0、1-3、3-5、5+、其他',
  `target_job` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标岗位',
  `expected_salary` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '期望薪资',
  `expected_city` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '期望城市',
  `skill` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '核心技能',
  `resume_path` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '简历路径',
  `resume_submitted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否通过简历上传完成采集',
  `resume_claims` text COLLATE utf8mb4_unicode_ci COMMENT '简历摘录',
  `status` tinyint DEFAULT '0' COMMENT '状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_target_job` (`target_job`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测评参与者';

CREATE TABLE `assessment_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话 ID',
  `participant_id` int NOT NULL COMMENT '参与者 ID',
  `experience_version` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '体验版本标签',
  `current_phase` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'COLLECT' COMMENT '当前 workflow 阶段',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS/COMPLETED/ABANDONED',
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_participant_id` (`participant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_participant_started` (`participant_id`, `started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测评会话';

CREATE TABLE `assessment_artifact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话 ID',
  `participant_id` int NOT NULL COMMENT '参与者 ID',
  `artifact_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产物类型',
  `content` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'JSON 或文本内容',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_type_created` (`session_id`, `artifact_type`, `created_at`),
  KEY `idx_participant_session` (`participant_id`, `session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测评产物（技能/职业/报告/反馈）';

CREATE TABLE `assessment_outcome` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话 ID',
  `participant_id` int NOT NULL COMMENT '参与者 ID',
  `skill_adjusted` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '调整后技能',
  `holland_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '霍兰德代码',
  `salary_adjusted` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '调整后薪资',
  `job_adjusted` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '调整后岗位',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_participant_id` (`participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测评结果快照（按会话）';

CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话 ID',
  `participant_id` int NOT NULL COMMENT '参与者 ID',
  `role` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'USER / ASSISTANT',
  `agent_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Agent 类型',
  `message` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `token_usage` int DEFAULT '0' COMMENT 'token 消耗量',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_created` (`session_id`, `created_at`),
  KEY `idx_participant_id` (`participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息日志';

CREATE TABLE `flow_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话 ID',
  `participant_id` int DEFAULT NULL COMMENT '参与者 ID',
  `from_phase` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原阶段',
  `to_phase` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标阶段',
  `trigger_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '触发器',
  `detail` text COLLATE utf8mb4_unicode_ci COMMENT '详情',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_created` (`session_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Workflow 阶段变更审计';

SET FOREIGN_KEY_CHECKS = 1;
