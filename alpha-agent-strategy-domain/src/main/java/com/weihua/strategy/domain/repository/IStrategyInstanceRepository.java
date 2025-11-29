package com.weihua.strategy.domain.repository;

import com.weihua.strategy.domain.model.aggregate.StrategyInstanceAggregate;
import com.weihua.strategy.domain.model.valobj.StrategyStatus;

import java.util.List;

public interface IStrategyInstanceRepository {
    void save(StrategyInstanceAggregate aggregate);
    void updateStatus(String strategyId, StrategyStatus status);
    StrategyInstanceAggregate findByStrategyId(String strategyId);
    List<StrategyInstanceAggregate> findByStatus(StrategyStatus status);
    List<StrategyInstanceAggregate> findByUserId(String userId);
    List<StrategyInstanceAggregate> findByUserIdAndStatus(String userId, StrategyStatus status);
}
