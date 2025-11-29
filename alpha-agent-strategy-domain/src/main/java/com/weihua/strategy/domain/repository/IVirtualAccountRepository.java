package com.weihua.strategy.domain.repository;

import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;

public interface IVirtualAccountRepository {
    VirtualAccountAggregate findByStrategyId(String strategyId, String currency);
    void save(VirtualAccountAggregate account);
}
