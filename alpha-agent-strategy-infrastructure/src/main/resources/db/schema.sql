/*
Navicat Premium Data Transfer

Source Server         : group_buy_market
Source Server Type    : MySQL
Source Server Version : 80032
Source Host           : localhost:13306
Source Schema         : valuecell-strategy

Target Server Type    : MySQL
Target Server Version : 80032
File Encoding         : 65001

Date: 29/11/2025 21:39:13
*/

SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_decision_logs
-- ----------------------------
DROP TABLE IF EXISTS `t_decision_logs`;

CREATE TABLE `t_decision_logs` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `strategy_instance_id` bigint NOT NULL COMMENT '关联的策略实例ID',
    `cycle_id` varchar(64) DEFAULT NULL COMMENT '决策循环ID(UUID)',
    `prompt_snapshot` text COMMENT '发送给LLM的完整Prompt',
    `llm_response_json` text COMMENT 'LLM返回的原始JSON',
    `rationale` text COMMENT 'LLM给出的决策理由',
    `instructions_json` text COMMENT '决策指令JSON',
    `decision_time` datetime DEFAULT NULL COMMENT '决策时间',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_strategy_instance` (`strategy_instance_id`),
    KEY `idx_cycle_id` (`cycle_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 89 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '决策日志表';

-- ----------------------------
-- Table structure for t_market_history
-- ----------------------------
DROP TABLE IF EXISTS `t_market_history`;

CREATE TABLE `t_market_history` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `strategy_instance_id` bigint NOT NULL COMMENT '关联的策略实例ID',
    `symbol` varchar(32) NOT NULL COMMENT '交易对',
    `price` decimal(20, 8) DEFAULT NULL COMMENT '价格',
    `rsi` decimal(20, 8) DEFAULT NULL COMMENT 'RSI指标',
    `funding_rate` decimal(20, 8) DEFAULT NULL COMMENT '资金费率',
    `indicators_json` text COMMENT '其他技术指标JSON',
    `recorded_at` datetime DEFAULT NULL COMMENT '记录时间(业务时间)',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_strategy_instance` (`strategy_instance_id`),
    KEY `idx_recorded_at` (`recorded_at`)
) ENGINE = InnoDB AUTO_INCREMENT = 59 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '市场数据历史表';

-- ----------------------------
-- Table structure for t_portfolio_snapshots
-- ----------------------------
DROP TABLE IF EXISTS `t_portfolio_snapshots`;

CREATE TABLE `t_portfolio_snapshots` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `strategy_instance_id` bigint NOT NULL COMMENT '关联的策略实例ID',
    `decision_log_id` bigint DEFAULT NULL COMMENT '关联的决策日志ID',
    `total_balance` decimal(20, 8) DEFAULT NULL COMMENT '总资产估值',
    `available_balance` decimal(20, 8) DEFAULT NULL COMMENT '可用余额',
    `positions_json` text COMMENT '持仓详情JSON列表',
    `recorded_at` datetime DEFAULT NULL COMMENT '记录时间',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_strategy_instance` (`strategy_instance_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 89 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账户持仓快照表';

-- ----------------------------
-- Table structure for t_prompt_templates
-- ----------------------------
DROP TABLE IF EXISTS `t_prompt_templates`;

CREATE TABLE `t_prompt_templates` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '物理主键ID',
    `template_id` varchar(64) NOT NULL COMMENT '业务模板ID (UUID)',
    `name` varchar(128) DEFAULT NULL COMMENT '模板名称',
    `description` text COMMENT '模板描述',
    `content` text COMMENT 'Prompt 模板内容',
    `category` varchar(64) DEFAULT NULL COMMENT '分类',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_id` (`template_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 2 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Prompt模板表';

-- ----------------------------
-- Table structure for t_strategy_accounts
-- ----------------------------
DROP TABLE IF EXISTS `t_strategy_accounts`;

CREATE TABLE `t_strategy_accounts` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `strategy_instance_id` bigint NOT NULL COMMENT '关联的策略实例ID',
    `currency` varchar(16) NOT NULL COMMENT '币种',
    `balance` decimal(20, 8) DEFAULT '0.00000000' COMMENT '总余额',
    `frozen` decimal(20, 8) DEFAULT '0.00000000' COMMENT '冻结金额',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_strategy_instance` (`strategy_instance_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 2 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '策略账户表';

-- ----------------------------
-- Table structure for t_strategy_instances
-- ----------------------------
DROP TABLE IF EXISTS `t_strategy_instances`;

CREATE TABLE `t_strategy_instances` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `strategy_id` varchar(64) NOT NULL COMMENT '业务ID (UUID)',
    `user_id` varchar(64) DEFAULT NULL COMMENT '用户ID',
    `strategy_name` varchar(128) DEFAULT NULL COMMENT '策略名称',
    `strategy_type` varchar(32) DEFAULT NULL COMMENT '策略类型: PROMPT, GRID',
    `status` varchar(32) DEFAULT NULL COMMENT '状态: RUNNING, STOPPED, PAUSED, ERROR',
    `exchange_id` varchar(32) DEFAULT NULL COMMENT '交易所ID',
    `trading_mode` varchar(32) DEFAULT NULL COMMENT '交易模式: VIRTUAL, LIVE',
    `interval_seconds` int DEFAULT NULL COMMENT '决策间隔(秒)',
    `template_id` varchar(64) DEFAULT NULL COMMENT '策略Prompt模板ID',
    `prompt_text` text COMMENT '完整策略指令',
    `config_json` text COMMENT '完整配置JSON',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_strategy_id` (`strategy_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 2 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '策略实例表';

-- ----------------------------
-- Table structure for t_strategy_positions
-- ----------------------------
DROP TABLE IF EXISTS `t_strategy_positions`;

CREATE TABLE `t_strategy_positions` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `strategy_instance_id` bigint NOT NULL COMMENT '关联的策略实例ID',
    `symbol` varchar(32) NOT NULL COMMENT '交易对',
    `quantity` decimal(20, 8) DEFAULT '0.00000000' COMMENT '持仓数量',
    `avg_price` decimal(20, 8) DEFAULT '0.00000000' COMMENT '持仓均价',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_strategy_instance` (`strategy_instance_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 2 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '策略持仓表';

-- ----------------------------
-- Table structure for t_trade_executions
-- ----------------------------
DROP TABLE IF EXISTS `t_trade_executions`;

CREATE TABLE `t_trade_executions` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `instruction_id` varchar(64) DEFAULT NULL COMMENT '指令ID(UUID)',
    `strategy_instance_id` bigint NOT NULL COMMENT '关联的策略实例ID',
    `decision_log_id` bigint DEFAULT NULL COMMENT '关联的决策日志ID',
    `symbol` varchar(32) NOT NULL COMMENT '交易对',
    `action` varchar(32) DEFAULT NULL COMMENT '动作: BUY, SELL',
    `quantity` decimal(20, 8) DEFAULT NULL COMMENT '数量',
    `price` decimal(20, 8) DEFAULT NULL COMMENT '成交价格',
    `fee` decimal(20, 8) DEFAULT NULL COMMENT '手续费',
    `status` varchar(32) DEFAULT NULL COMMENT '状态: PENDING, FILLED, FAILED',
    `execution_time` datetime DEFAULT NULL COMMENT '执行时间',
    `execution_result_json` text COMMENT '执行结果详情',
    `error_message` text COMMENT '错误信息',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_strategy_instance` (`strategy_instance_id`),
    KEY `idx_instruction_id` (`instruction_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 5 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '交易执行表';

SET FOREIGN_KEY_CHECKS = 1;