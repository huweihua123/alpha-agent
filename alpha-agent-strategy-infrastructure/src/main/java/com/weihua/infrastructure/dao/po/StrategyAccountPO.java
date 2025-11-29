package com.weihua.infrastructure.dao.po;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略账户持久化对象
 */
@Data
public class StrategyAccountPO {
    /** 主键ID */
    private Long id;
    /** 关联的策略实例ID */
    private Long strategyInstanceId;
    /** 币种 */
    private String currency;
    /** 总余额 */
    private BigDecimal balance;
    /** 冻结金额 */
    private BigDecimal frozen;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
