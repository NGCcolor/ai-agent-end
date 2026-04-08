# ************************************************************
# AI Agent Station - 升级版完整数据结构与数据
# 包含了基础 Agent 以及 AutoAgent (多智能体流) 的完整配置
# ************************************************************

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE='NO_AUTO_VALUE_ON_ZERO', SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE database if NOT EXISTS `ai-agent-station-study` default character set utf8mb4 collate utf8mb4_0900_ai_ci;
use `ai-agent-station-study`;

# ============================================================
# 1. 转储表 ai_agent
# ============================================================
DROP TABLE IF EXISTS `ai_agent`;
CREATE TABLE `ai_agent` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `agent_id` varchar(64) NOT NULL COMMENT '智能体ID',
                            `agent_name` varchar(50) NOT NULL COMMENT '智能体名称',
                            `description` varchar(255) DEFAULT NULL COMMENT '描述',
                            `channel` varchar(32) DEFAULT NULL COMMENT '渠道类型(agent，chat_stream)',
                            `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI智能体配置表';

LOCK TABLES `ai_agent` WRITE;
/*!40000 ALTER TABLE `ai_agent` DISABLE KEYS */;
INSERT INTO `ai_agent` (`id`, `agent_id`, `agent_name`, `description`, `channel`, `status`, `create_time`, `update_time`)
VALUES
    (6,'1','自动发帖服务01','CSDN自动发帖，微信公众号通知。','agent',1,'2025-06-14 12:41:20','2025-06-14 12:41:20'),
    (7,'2','智能对话体（MCP）','自动发帖，工具服务','chat_stream',1,'2025-06-14 12:41:20','2025-06-14 12:41:20'),
    (8,'3','智能对话体（Auto）','多Agent自动分析和执行任务','agent',1,'2025-06-14 12:41:20','2025-06-14 12:41:20');
/*!40000 ALTER TABLE `ai_agent` ENABLE KEYS */;
UNLOCK TABLES;


# ============================================================
# 2. 转储表 ai_agent_flow_config (升级了最新表结构!)
# ============================================================
DROP TABLE IF EXISTS `ai_agent_flow_config`;
CREATE TABLE `ai_agent_flow_config` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                        `agent_id` varchar(64) NOT NULL COMMENT '智能体ID',
                                        `client_id` varchar(64) NOT NULL COMMENT '客户端ID',
                                        `client_name` varchar(64) DEFAULT NULL COMMENT '客户端名称',
                                        `client_type` varchar(64) DEFAULT NULL COMMENT '客户端类型',
                                        `sequence` int NOT NULL COMMENT '序列号(执行顺序)',
                                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uk_agent_client_seq` (`agent_id`,`client_id`,`sequence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能体-客户端关联表';

LOCK TABLES `ai_agent_flow_config` WRITE;
/*!40000 ALTER TABLE `ai_agent_flow_config` DISABLE KEYS */;
INSERT INTO `ai_agent_flow_config` (`id`, `agent_id`, `client_id`, `client_name`, `client_type`, `sequence`, `create_time`)
VALUES
    (1,'1','3001','通用的','DEFAULT',1,'2025-06-14 12:42:20'),
    (2,'3','3101','任务分析和状态判断','TASK_ANALYZER_CLIENT',1,'2025-06-14 12:42:20'),
    (3,'3','3102','具体任务执行','PRECISION_EXECUTOR_CLIENT',2,'2025-06-14 12:42:20'),
    (4,'3','3103','质量检查和优化','QUALITY_SUPERVISOR_CLIENT',3,'2025-06-14 12:42:20');
/*!40000 ALTER TABLE `ai_agent_flow_config` ENABLE KEYS */;
UNLOCK TABLES;


# ============================================================
# 3. 转储表 ai_agent_task_schedule
# ============================================================
DROP TABLE IF EXISTS `ai_agent_task_schedule`;
CREATE TABLE `ai_agent_task_schedule` (
                                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                          `agent_id` bigint NOT NULL COMMENT '智能体ID',
                                          `task_name` varchar(64) DEFAULT NULL COMMENT '任务名称',
                                          `description` varchar(255) DEFAULT NULL COMMENT '任务描述',
                                          `cron_expression` varchar(50) NOT NULL COMMENT '时间表达式',
                                          `task_param` text COMMENT '任务入参配置(JSON格式)',
                                          `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:无效,1:有效)',
                                          `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          PRIMARY KEY (`id`),
                                          KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能体任务调度配置表';

LOCK TABLES `ai_agent_task_schedule` WRITE;
/*!40000 ALTER TABLE `ai_agent_task_schedule` DISABLE KEYS */;
INSERT INTO `ai_agent_task_schedule` (`id`, `agent_id`, `task_name`, `description`, `cron_expression`, `task_param`, `status`, `create_time`, `update_time`)
VALUES
    (1,1,'自动发帖','自动发帖和通知','0 0/30 * * * ?','发布CSDN文章',1,'2025-06-14 12:44:05','2025-06-14 12:44:07');
/*!40000 ALTER TABLE `ai_agent_task_schedule` ENABLE KEYS */;
UNLOCK TABLES;


# ============================================================
# 4. 转储表 ai_client (包含 3 个新数字人)
# ============================================================
DROP TABLE IF EXISTS `ai_client`;
CREATE TABLE `ai_client` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                             `client_id` varchar(64) NOT NULL COMMENT '客户端ID',
                             `client_name` varchar(50) NOT NULL COMMENT '客户端名称',
                             `description` varchar(1024) DEFAULT NULL COMMENT '描述',
                             `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI客户端配置表';

LOCK TABLES `ai_client` WRITE;
/*!40000 ALTER TABLE `ai_client` DISABLE KEYS */;
INSERT INTO `ai_client` (`id`, `client_id`, `client_name`, `description`, `status`, `create_time`, `update_time`)
VALUES
    (1,'3001','提示词优化','提示词优化，分为角色、动作、规则、目标等。',1,'2025-06-14 12:34:36','2025-06-14 12:34:39'),
    (7,'3002','自动发帖和通知','自动生成CSDN文章，发送微信公众号消息通知',1,'2025-06-14 12:43:02','2025-06-14 12:43:02'),
    (8,'3003','文件操作服务','文件操作服务',1,'2025-06-14 12:43:02','2025-06-14 12:43:02'),
    (9,'3004','流式对话客户端','流式对话客户端',1,'2025-06-14 12:43:02','2025-06-14 12:43:02'),
    (10,'3005','地图','地图',1,'2025-06-14 12:43:02','2025-06-14 12:43:02'),
    (11,'3101','任务分析和状态判断','AutoAgent Task Analyzer',1,'2025-06-14 12:43:02','2025-06-14 12:43:02'),
    (12,'3102','具体任务执行','AutoAgent Precision Executor',1,'2025-06-14 12:43:02','2025-06-14 12:43:02'),
    (13,'3103','质量检查和优化','AutoAgent Quality Supervisor',1,'2025-06-14 12:43:02','2025-06-14 12:43:02');
/*!40000 ALTER TABLE `ai_client` ENABLE KEYS */;
UNLOCK TABLES;


# ============================================================
# 5. 转储表 ai_client_advisor
# ============================================================
DROP TABLE IF EXISTS `ai_client_advisor`;
CREATE TABLE `ai_client_advisor` (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                     `advisor_id` varchar(64) NOT NULL COMMENT '顾问ID',
                                     `advisor_name` varchar(50) NOT NULL COMMENT '顾问名称',
                                     `advisor_type` varchar(50) NOT NULL COMMENT '顾问类型',
                                     `order_num` int DEFAULT '0' COMMENT '顺序号',
                                     `ext_param` varchar(2048) DEFAULT NULL COMMENT '扩展参数配置，json 记录',
                                     `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_advisor_id` (`advisor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='顾问配置表';

LOCK TABLES `ai_client_advisor` WRITE;
/*!40000 ALTER TABLE `ai_client_advisor` DISABLE KEYS */;
INSERT INTO `ai_client_advisor` (`id`, `advisor_id`, `advisor_name`, `advisor_type`, `order_num`, `ext_param`, `status`, `create_time`, `update_time`)
VALUES
    (1,'4001','记忆','ChatMemory',1,'{\n    \"maxMessages\": 200\n}',1,'2025-06-14 12:35:06','2025-06-14 12:35:44'),
    (2,'4002','访问文章提示词知识库','RagAnswer',1,'{\n    \"topK\": \"4\",\n    \"filterExpression\": \"knowledge == \'知识库名称\'\"\n}',1,'2025-06-14 12:35:06','2025-06-14 12:35:44');
/*!40000 ALTER TABLE `ai_client_advisor` ENABLE KEYS */;
UNLOCK TABLES;


# ============================================================
# 6. 转储表 ai_client_api
# ============================================================
DROP TABLE IF EXISTS `ai_client_api`;
CREATE TABLE `ai_client_api` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
                                 `api_id` varchar(64) NOT NULL COMMENT '全局唯一配置ID',
                                 `base_url` varchar(255) NOT NULL COMMENT 'API基础URL',
                                 `api_key` varchar(255) NOT NULL COMMENT 'API密钥',
                                 `completions_path` varchar(255) NOT NULL COMMENT '补全API路径',
                                 `embeddings_path` varchar(255) NOT NULL COMMENT '嵌入API路径',
                                 `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_api_id` (`api_id`),
                                 KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OpenAI API配置表';

LOCK TABLES `ai_client_api` WRITE;
/*!40000 ALTER TABLE `ai_client_api` DISABLE KEYS */;
INSERT INTO `ai_client_api` (`id`, `api_id`, `base_url`, `api_key`, `completions_path`, `embeddings_path`, `status`, `create_time`, `update_time`)
VALUES
    (1,'1001','https://apis.itedus.cn','sk-lIqVNiHon00O6veJ15Cc57DaF5Dd401f93B3A107B4B3677e','v1/chat/completions','v1/embeddings',1,'2025-06-14 12:33:22','2025-06-14 12:33:22');
/*!40000 ALTER TABLE `ai_client_api` ENABLE KEYS */;
UNLOCK TABLES;


# ============================================================
# 7. 转储表 ai_client_config (加入了全新数字人的绑定关系)
# ============================================================
DROP TABLE IF EXISTS `ai_client_config`;
CREATE TABLE `ai_client_config` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                    `source_type` varchar(32) NOT NULL COMMENT '源类型（model、client）',
                                    `source_id` varchar(64) NOT NULL COMMENT '源ID',
                                    `target_type` varchar(32) NOT NULL COMMENT '目标类型',
                                    `target_id` varchar(64) NOT NULL COMMENT '目标ID',
                                    `ext_param` varchar(1024) DEFAULT NULL COMMENT '扩展参数',
                                    `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_source_id` (`source_id`),
                                    KEY `idx_target_id` (`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI客户端统一关联配置表';

LOCK TABLES `ai_client_config` WRITE;
/*!40000 ALTER TABLE `ai_client_config` DISABLE KEYS */;
INSERT INTO `ai_client_config` (`id`, `source_type`, `source_id`, `target_type`, `target_id`, `ext_param`, `status`, `create_time`, `update_time`)
VALUES
    (1,'model','2001','tool_mcp','5001','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:47:43'),
    (2,'model','2001','tool_mcp','5002','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:47:43'),
    (3,'model','2001','tool_mcp','5003','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:47:43'),
    (4,'model','2001','tool_mcp','5004','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:47:43'),
    (5,'client','3001','advisor','4001','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:49:46'),
    (6,'client','3001','prompt','6001','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:50:13'),
    (7,'client','3001','prompt','6002','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:50:13'),
    (8,'client','3001','model','2001','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:50:13'),
    (9,'model','2001','tool_mcp','5006','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:47:43'),
    (10,'client','3101','model','2001','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:46:49'),
    (11,'client','3101','prompt','6101','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:46:49'),
    (12,'client','3101','advisor','4001','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:46:49'),
    (13,'client','3101','tool_mcp','5006','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:46:49'),
    (14,'client','3102','model','2001','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:46:49'),
    (15,'client','3102','prompt','6102','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:46:49'),
    (16,'client','3102','advisor','4001','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:46:49'),
    (17,'client','3102','tool_mcp','5006','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:46:49'),
    (18,'client','3103','model','2001','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:46:49'),
    (19,'client','3103','prompt','6103','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:46:49'),
    (20,'client','3103','advisor','4001','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:46:49'),
    (21,'client','3103','tool_mcp','5006','\"\"',1,'2025-06-14 12:46:49','2025-06-14 12:46:49');
/*!40000 ALTER TABLE `ai_client_config` ENABLE KEYS */;
UNLOCK TABLES;


# ============================================================
# 8. 转储表 ai_client_model (🔥已自动修改为 qwen-max🔥)
# ============================================================
DROP TABLE IF EXISTS `ai_client_model`;
CREATE TABLE `ai_client_model` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
                                   `model_id` varchar(64) NOT NULL COMMENT '全局唯一模型ID',
                                   `api_id` varchar(64) NOT NULL COMMENT '关联的API配置ID',
                                   `model_name` varchar(64) NOT NULL COMMENT '模型名称',
                                   `model_type` varchar(32) NOT NULL COMMENT '模型类型：openai、deepseek、claude',
                                   `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_model_id` (`model_id`),
                                   KEY `idx_api_config_id` (`api_id`),
                                   KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天模型配置表';

LOCK TABLES `ai_client_model` WRITE;
/*!40000 ALTER TABLE `ai_client_model` DISABLE KEYS */;
INSERT INTO `ai_client_model` (`id`, `model_id`, `api_id`, `model_name`, `model_type`, `status`, `create_time`, `update_time`)
VALUES
    (1,'2001','1001','qwen-max','openai',1,'2025-06-14 12:33:47','2025-06-14 12:33:47');
/*!40000 ALTER TABLE `ai_client_model` ENABLE KEYS */;
UNLOCK TABLES;


# ============================================================
# 9. 转储表 ai_client_rag_order
# ============================================================
DROP TABLE IF EXISTS `ai_client_rag_order`;
CREATE TABLE `ai_client_rag_order` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                       `rag_id` varchar(50) NOT NULL COMMENT '知识库ID',
                                       `rag_name` varchar(50) NOT NULL COMMENT '知识库名称',
                                       `knowledge_tag` varchar(50) NOT NULL COMMENT '知识标签',
                                       `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_rag_id` (`rag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库配置表';

LOCK TABLES `ai_client_rag_order` WRITE;
/*!40000 ALTER TABLE `ai_client_rag_order` DISABLE KEYS */;
INSERT INTO `ai_client_rag_order` (`id`, `rag_id`, `rag_name`, `knowledge_tag`, `status`, `create_time`, `update_time`)
VALUES
    (3,'9001','生成文章提示词','生成文章提示词',1,'2025-06-14 12:44:56','2025-06-14 12:44:56');
/*!40000 ALTER TABLE `ai_client_rag_order` ENABLE KEYS */;
UNLOCK TABLES;


# ============================================================
# 10. 转储表 ai_client_system_prompt (加入 3个新角色的灵魂)
# ============================================================
DROP TABLE IF EXISTS `ai_client_system_prompt`;
CREATE TABLE `ai_client_system_prompt` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                           `prompt_id` varchar(64) NOT NULL COMMENT '提示词ID',
                                           `prompt_name` varchar(50) NOT NULL COMMENT '提示词名称',
                                           `prompt_content` text NOT NULL COMMENT '提示词内容',
                                           `description` varchar(1024) DEFAULT NULL COMMENT '描述',
                                           `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
                                           `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           PRIMARY KEY (`id`),
                                           UNIQUE KEY `uk_prompt_id` (`prompt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统提示词配置表';

LOCK TABLES `ai_client_system_prompt` WRITE;
/*!40000 ALTER TABLE `ai_client_system_prompt` DISABLE KEYS */;
INSERT INTO `ai_client_system_prompt` (`id`, `prompt_id`, `prompt_name`, `prompt_content`, `description`, `status`, `create_time`, `update_time`)
VALUES
    (6,'6001','提示词优化','你是一个专业的AI提示词优化专家。请帮我优化以下prompt...','提示词优化',1,'2025-06-14 12:39:02','2025-06-14 12:39:02'),
    (7,'6002','发帖和消息通知介绍','你是一个 AI Agent 智能体，可以根据用户输入信息生成文章...','发帖通知',1,'2025-06-14 12:39:02','2025-06-14 12:39:02'),
    (8,'6003','CSDN发布文章','我需要你帮我生成一篇文章...','CSDN发布文章',1,'2025-06-14 12:39:02','2025-06-14 12:39:02'),
    (9,'6004','文章操作测试','在 /Users/fuzhengwei/Desktop 创建文件 file01.txt','文件操作测试',1,'2025-06-14 12:39:02','2025-06-14 12:39:02'),
    (10,'6101','负责任务分析和状态判断','# 角色\n你是一个专业的任务分析师，名叫 AutoAgent Task Analyzer。\n# 核心职责\n你的主要任务是深入理解用户的需求目标，基于当前已经获取的信息，规划并确定下一步需要执行的具体动作。','多Agent分析师',1,'2025-06-14 12:39:02','2025-06-14 12:39:02'),
    (11,'6102','负责具体任务执行','# 角色\n你是一个精准任务执行器，名叫 AutoAgent Precision Executor。\n# 核心能力\n你可以调用系统提供的各类 MCP 工具进行搜索、文件处理、发帖等具体操作。请严格执行任务。','多Agent执行器',1,'2025-06-14 12:39:02','2025-06-14 12:39:02'),
    (12,'6103','负责质量检查和优化','# 角色\n你是一个专业的质量监督员，名叫 AutoAgent Quality Supervisor。\n# 核心职责\n请仔细审查执行器提交的结果是否符合用户最初的需求，如果失败，请提供优化建议。','多Agent质检员',1,'2025-06-14 12:39:02','2025-06-14 12:39:02');
/*!40000 ALTER TABLE `ai_client_system_prompt` ENABLE KEYS */;
UNLOCK TABLES;


# ============================================================
# 11. 转储表 ai_client_tool_mcp (加入了 百度搜索 MCP)
# ============================================================
DROP TABLE IF EXISTS `ai_client_tool_mcp`;
CREATE TABLE `ai_client_tool_mcp` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                      `mcp_id` varchar(64) NOT NULL COMMENT 'MCP名称',
                                      `mcp_name` varchar(50) NOT NULL COMMENT 'MCP名称',
                                      `transport_type` varchar(20) NOT NULL COMMENT '传输类型(sse/stdio)',
                                      `transport_config` varchar(1024) DEFAULT NULL COMMENT '传输配置(sse/stdio)',
                                      `request_timeout` int DEFAULT '180' COMMENT '请求超时时间(分钟)',
                                      `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_mcp_id` (`mcp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MCP客户端配置表';

LOCK TABLES `ai_client_tool_mcp` WRITE;
/*!40000 ALTER TABLE `ai_client_tool_mcp` DISABLE KEYS */;
INSERT INTO `ai_client_tool_mcp` (`id`, `mcp_id`, `mcp_name`, `transport_type`, `transport_config`, `request_timeout`, `status`, `create_time`, `update_time`)
VALUES
    (6,'5001','CSDN自动发帖','sse','{\n	\"baseUri\":\"http://192.168.1.108:8101\",\n        \"sseEndpoint\":\"/sse\"\n}',180,1,'2025-06-14 12:36:30','2025-06-14 12:36:40'),
    (7,'5002','微信公众号消息通知','sse','{\n	\"baseUri\":\"http://192.168.1.108:8102\",\n        \"sseEndpoint\":\"/sse\"\n}',180,1,'2025-06-14 12:36:30','2025-06-14 12:36:40'),
    (8,'5003','本地文件操作','stdio','{\n    \"filesystem\": {\n        \"command\": \"npx\",\n        \"args\": [\n            \"-y\",\n            \"@modelcontextprotocol/server-filesystem\",\n            \"/Users/fuzhengwei/Desktop\",\n            \"/Users/fuzhengwei/Desktop\"\n        ]\n    }\n}',180,1,'2025-06-14 12:36:30','2025-06-14 12:36:40'),
    (9,'5004','g-search','stdio','{\n    \"g-search\": {\n        \"command\": \"npx\",\n        \"args\": [\n            \"-y\",\n            \"g-search-mcp\"\n        ]\n    }\n}',180,1,'2025-06-14 12:36:30','2025-06-14 12:36:40'),
    (10,'5005','高德地图','sse','{\n	\"baseUri\":\"https://mcp.amap.com\",\n        \"sseEndpoint\":\"/sse?key=801aabf79ed055c2ff78603cfe851787\"\n}',180,1,'2025-06-14 12:36:30','2025-06-14 12:36:40'),
    (11,'5006','baidu-search','sse','{\n	\"baseUri\":\"http://appbuilder.baidu.com/v2/ai_search/mcp/\",\n        \"sseEndpoint\":\"sse?api_key=Bearer+bce-v3/ALTAK-3zODLb9qHozIftQlGwez5/2696e92781f5bf1ba1870e2958f239fd6dc822a4\"\n}',180,1,'2025-06-14 12:36:30','2025-06-14 12:36:40');
/*!40000 ALTER TABLE `ai_client_tool_mcp` ENABLE KEYS */;
UNLOCK TABLES;


/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;