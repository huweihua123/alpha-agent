package com.weihua.infrastructure.dao.po;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略持仓持久化对象
 */
@Data
public class StrategyPositionPO {
    /** 主键ID */
    private Long id;
    /** 关联的策略实例ID */
    private Long strategyInstanceId;
    /** 交易对 */
    private String symbol;
    /** 持仓数量 */
    private BigDecimal quantity;
    /** 持仓均价 */
    private BigDecimal avgPrice;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
