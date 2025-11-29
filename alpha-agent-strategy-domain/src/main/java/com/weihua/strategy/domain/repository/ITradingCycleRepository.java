package com.weihua.strategy.domain.repository;

import com.weihua.strategy.domain.model.entity.DecisionEntity;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.entity.PortfolioSnapshotEntity;
import com.weihua.strategy.domain.model.entity.TradeExecutionEntity;

/**
 * 交易历史仓储接口
 * 负责保存交易循环中产生的历史记录
 */
public interface ITradingCycleRepository {
    
    /**
     * 保存市场快照
     */
    void saveMarketSnapshot(String strategyId, MarketSnapshotEntity snapshot);
    
    /**
     * 保存完整的交易周期聚合根
     * @param cycle 交易周期聚合根
     */
    void save(com.weihua.strategy.domain.model.aggregate.TradingCycleAggregate cycle);
}
