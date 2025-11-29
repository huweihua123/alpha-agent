package com.weihua.strategy.domain.service;

import com.weihua.strategy.domain.model.aggregate.StrategyInstanceAggregate;
import com.weihua.strategy.domain.model.entity.StrategyConfigEntity;

public interface IStrategyLifecycleService {
    Long createStrategy(StrategyConfigEntity config);
    void startStrategy(Long strategyId);
    void stopStrategy(Long strategyId);
    void pauseStrategy(Long strategyId);
    void updateConfig(Long strategyId, StrategyConfigEntity newConfig);
    StrategyInstanceAggregate getStrategy(Long strategyId);
}
