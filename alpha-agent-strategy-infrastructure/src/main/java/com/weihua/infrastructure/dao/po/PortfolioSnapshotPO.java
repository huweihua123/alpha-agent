package com.weihua.infrastructure.dao.po;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户持仓快照持久化对象
 */
@Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class PortfolioSnapshotPO {
    /** 主键ID */
    private Long id;
    /** 关联的策略实例ID */
    private Long strategyInstanceId;
    /** 关联的决策日志ID */
    private Long decisionLogId;
    /** 总资产估值 */
    private BigDecimal totalBalance;
    /** 可用余额 */
    private BigDecimal availableBalance;
    /** 持仓详情JSON列表 */
    private String positionsJson;
    /** 记录时间 */
    private LocalDateTime recordedAt;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
