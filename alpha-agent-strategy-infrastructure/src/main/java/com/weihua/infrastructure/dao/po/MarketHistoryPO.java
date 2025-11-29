package com.weihua.infrastructure.dao.po;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 市场数据历史持久化对象
 */
@Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class MarketHistoryPO {
    /** 主键ID */
    private Long id;
    /** 关联的策略实例ID */
    private Long strategyInstanceId;
    /** 交易对 */
    private String symbol;
    /** 价格 */
    private BigDecimal price;
    /** RSI指标 */
    private BigDecimal rsi;
    /** 资金费率 */
    private BigDecimal fundingRate;
    /** 其他技术指标JSON */
    private String indicatorsJson;
    /** 记录时间(业务时间) */
    private LocalDateTime recordedAt;
    /** 创建时间(系统时间) */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
