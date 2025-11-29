package com.weihua.strategy.domain.adapter.port;

import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.entity.PortfolioSnapshotEntity;
import com.weihua.strategy.domain.model.entity.StrategyConfigEntity;
import com.weihua.strategy.domain.model.entity.TradePlan;

/**
 * LLM 端口
 * 负责与 LLM 交互获取决策
 */
public interface ILlmPort {
    /**
     * 获取交易计划 (Raw Plan)
     */
    TradePlan askForPlan(
            String strategyId,
            MarketSnapshotEntity market,
            PortfolioSnapshotEntity portfolio,
            StrategyConfigEntity config
    );
}
