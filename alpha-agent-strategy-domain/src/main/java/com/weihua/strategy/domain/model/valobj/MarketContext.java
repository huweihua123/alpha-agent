package com.weihua.strategy.domain.model.valobj;

import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 市场上下文
 * 包含决策所需的所有数据：行情、账户状态、策略配置等
 */
@Getter
@Builder
public class MarketContext {
    private final String strategyId;
    private final Map<String, MarketSnapshotEntity> marketData;
    private final MarketSnapshotEntity primaryMarket;
    private final VirtualAccountAggregate account;
}
