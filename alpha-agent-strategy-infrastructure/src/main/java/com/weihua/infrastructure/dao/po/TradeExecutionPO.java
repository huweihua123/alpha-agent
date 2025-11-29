package com.weihua.infrastructure.dao.po;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易执行持久化对象
 */
@Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class TradeExecutionPO {
    /** 主键ID */
    private Long id;
    /** 指令ID(UUID) */
    private String instructionId;
    /** 关联的策略实例ID */
    private Long strategyInstanceId;
    /** 关联的决策日志ID */
    private Long decisionLogId;
    /** 交易对 */
    private String symbol;
    /** 动作: BUY, SELL, OPEN_LONG, etc. */
    private String action;
    /** 数量 */
    private BigDecimal quantity;
    /** 成交价格 */
    private BigDecimal price;
    /** 手续费 */
    private BigDecimal fee;
    /** 状态: PENDING, FILLED, FAILED, PARTIALLY_FILLED */
    private String status;
    /** 执行时间 */
    private LocalDateTime executionTime;
    /** 执行结果详情(成交价、手续费等) */
    private String executionResultJson;
    /** 错误信息 */
    private String errorMessage;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
