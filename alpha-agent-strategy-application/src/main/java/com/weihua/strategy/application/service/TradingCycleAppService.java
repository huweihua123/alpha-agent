package com.weihua.strategy.application.service;

import com.weihua.strategy.domain.model.entity.DecisionEntity;
import com.weihua.strategy.domain.model.valobj.MarketContext;
import com.weihua.strategy.domain.service.MarketAnalysisDomainService;
import com.weihua.strategy.domain.service.StrategyComposerDomainService;
import com.weihua.strategy.domain.service.TradeExecutionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 应用服务：交易循环
 * 负责编排交易流程，不包含具体业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingCycleAppService {

    private final MarketAnalysisDomainService marketService;
    private final StrategyComposerDomainService composerService;
    private final TradeExecutionDomainService executionService;

    /**
     * 执行一次交易循环
     */
    public void executeCycle(String strategyId) {
        log.info("Starting trading cycle for strategy: {}", strategyId);

        try {
            // 1. 编排：准备上下文 (行情、账户、配置)
            MarketContext context = marketService.prepareContext(strategyId);
            if (context == null) {
                log.warn("Skipping cycle: context preparation failed.");
                return;
            }

            // 2. 编排：制定决策 (LLM + 风控)
            DecisionEntity decision = composerService.makeDecision(context);

            // 3. 编排：执行交易 (下单 + 持久化)
            executionService.executeAndPersist(decision, context);

        } catch (Exception e) {
            log.error("Error during trading cycle execution for strategy: {}", strategyId, e);
            // 这里可以添加告警通知逻辑
        }
    }
}
