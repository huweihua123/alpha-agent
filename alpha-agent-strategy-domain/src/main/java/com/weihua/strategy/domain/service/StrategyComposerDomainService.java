package com.weihua.strategy.domain.service;

import com.weihua.strategy.domain.adapter.port.ILlmPort;
import com.weihua.strategy.domain.model.aggregate.StrategyInstanceAggregate;
import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;
import com.weihua.strategy.domain.model.entity.DecisionEntity;
import com.weihua.strategy.domain.model.entity.PortfolioSnapshotEntity;
import com.weihua.strategy.domain.model.entity.TradePlan;
import com.weihua.strategy.domain.model.valobj.MarketContext;
import com.weihua.strategy.domain.repository.IStrategyInstanceRepository;
import com.weihua.strategy.domain.service.rule.TradeRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 领域服务：策略决策
 * 负责协调 LLM 和风控规则，生成最终决策
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyComposerDomainService {

    private final ILlmPort llmPort;
    private final TradeRuleService tradeRuleService;
    private final IStrategyInstanceRepository strategyInstanceRepository;

    /**
     * 制定决策
     */
    public DecisionEntity makeDecision(MarketContext context) {
        // 重新加载策略配置以确保是最新的
        StrategyInstanceAggregate strategy = strategyInstanceRepository.findByStrategyId(context.getStrategyId());
        
        // 构建资产快照供 LLM 参考
        PortfolioSnapshotEntity portfolioSnapshot = buildPortfolioSnapshot(context.getAccount());

        // 1. 调用 LLM 获取交易计划
        TradePlan plan = llmPort.askForPlan(
                context.getStrategyId(), 
                context.getPrimaryMarket(), 
                portfolioSnapshot, 
                strategy.getConfig()
        );

        // 2. 风控规则过滤
        DecisionEntity decision = tradeRuleService.filter(plan, context.getAccount(), context.getPrimaryMarket());
        
        log.info("Decision made for strategy {}: {}", context.getStrategyId(), decision.getRationale());
        return decision;
    }

    private PortfolioSnapshotEntity buildPortfolioSnapshot(VirtualAccountAggregate account) {
        List<PortfolioSnapshotEntity.PositionEntity> positionSnapshots = account.getPositions().values().stream()
                .map(p -> PortfolioSnapshotEntity.PositionEntity.builder()
                        .symbol(p.getSymbol())
                        .quantity(p.getQuantity())
                        .entryPrice(p.getAvgPrice())
                        .unrealizedPnl(BigDecimal.ZERO) // 暂不计算未实现盈亏
                        .build())
                .collect(Collectors.toList());

        return PortfolioSnapshotEntity.builder()
                .totalBalance(account.getBalance()) 
                .availableBalance(account.getBalance())
                .positions(positionSnapshots)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
