package com.weihua.infrastructure.dao.po;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 策略实例持久化对象
 */
@Data
public class StrategyInstancePO {
    /** 主键ID */
    private Long id;
    /** 业务ID (UUID) */
    private String strategyId;
    /** 用户ID */
    private String userId;
    /** 策略名称 */
    private String strategyName;
    /** 策略类型: PROMPT, GRID */
    private String strategyType;
    /** 状态: RUNNING, STOPPED, PAUSED, ERROR */
    private String status;
    /** 交易所ID */
    private String exchangeId;
    /** 交易模式: VIRTUAL, LIVE */
    private String tradingMode;
    /** 决策间隔(秒) */
    private Integer intervalSeconds;
    /** 策略Prompt模板ID */
    private String templateId;
    /** 完整策略指令 */
    private String promptText;
    /** 完整配置JSON(包含symbols, llmConfig, exchangeConfig等) */
    private String configJson;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
