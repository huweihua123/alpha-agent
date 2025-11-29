package com.weihua.strategy.domain.service;

import com.weihua.strategy.domain.model.aggregate.TradingCycleAggregate;
import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;
import com.weihua.strategy.domain.model.entity.DecisionEntity;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.entity.PortfolioSnapshotEntity;
import com.weihua.strategy.domain.model.entity.TradeExecutionEntity;
import com.weihua.strategy.domain.model.entity.TradeInstruction;
import com.weihua.strategy.domain.model.valobj.ExecutionStatus;
import com.weihua.strategy.domain.model.valobj.MarketContext;
import com.weihua.strategy.domain.model.valobj.TradeAction;
import com.weihua.strategy.domain.repository.ITradingCycleRepository;
import com.weihua.strategy.domain.repository.IVirtualAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 领域服务：交易执行
 * 负责执行决策并持久化结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeExecutionDomainService {

    private final IVirtualAccountRepository virtualAccountRepository;
    private final ITradingCycleRepository tradingCycleRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * 执行并持久化
     */
    public void executeAndPersist(DecisionEntity decision, MarketContext context) {
        String cycleId = UUID.randomUUID().toString();
        String strategyId = context.getStrategyId();
        VirtualAccountAggregate account = context.getAccount();
        MarketSnapshotEntity primaryMarket = context.getPrimaryMarket();
        Map<String, MarketSnapshotEntity> marketData = context.getMarketData();

        transactionTemplate.execute(status -> {
            // 1. 执行指令
            List<TradeExecutionEntity> executions = decision.getInstructions().stream()
                    .map(instruction -> executeInstruction(instruction, marketData, primaryMarket, account))
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            // 2. 更新账户
            virtualAccountRepository.save(account);
            
            // 3. 构建最新的资产快照 (交易后)
            PortfolioSnapshotEntity latestPortfolio = buildPortfolioSnapshot(account);

            // 4. 保存交易周期记录
            TradingCycleAggregate cycle = TradingCycleAggregate.builder()
                    .cycleId(cycleId)
                    .strategyId(strategyId)
                    .startTime(LocalDateTime.now()) // 简化处理，实际应记录开始时间
                    .endTime(LocalDateTime.now())
                    .marketSnapshot(primaryMarket)
                    .decision(decision)
                    .executions(executions)
                    .portfolioSnapshot(latestPortfolio)
                    .build();

            tradingCycleRepository.save(cycle);
            return null;
        });
        
        log.info("Completed trading cycle execution: {}, trades: {}", cycleId, decision.getInstructions().size());
    }

    /**
     * 执行单个交易指令
     */
    private TradeExecutionEntity executeInstruction(
            TradeInstruction instruction,
            Map<String, MarketSnapshotEntity> marketData,
            MarketSnapshotEntity primaryMarket,
            VirtualAccountAggregate account
    ) {
        String instructionId = UUID.randomUUID().toString();
        LocalDateTime executionTime = LocalDateTime.now();
        
        try {
            // 确保有对应的价格
            MarketSnapshotEntity symbolMarket = marketData.get(instruction.getSymbol());
            BigDecimal currentPrice = (symbolMarket != null) ? symbolMarket.getPrice() : primaryMarket.getPrice();
            
            // 计算手续费 (假设 0.1%)
            BigDecimal fee = instruction.getQuantity().multiply(currentPrice).multiply(new BigDecimal("0.001"));
            
            // 执行交易
            TradeAction action = instruction.getAction();
            
            if (TradeAction.BUY == action) {
                account.buy(instruction.getSymbol(), instruction.getQuantity(), currentPrice, fee);
                log.info("Executed BUY: {} {} @ {} (Fee: {})", instruction.getQuantity(), instruction.getSymbol(), currentPrice, fee);
                
                return TradeExecutionEntity.builder()
                        .instructionId(instructionId)
                        .symbol(instruction.getSymbol())
                        .action(TradeAction.BUY)
                        .quantity(instruction.getQuantity())
                        .price(currentPrice)
                        .fee(fee)
                        .status(ExecutionStatus.FILLED)
                        .executionTime(executionTime)
                        .build();
                        
            } else if (TradeAction.SELL == action) {
                account.sell(instruction.getSymbol(), instruction.getQuantity(), currentPrice, fee);
                log.info("Executed SELL: {} {} @ {} (Fee: {})", instruction.getQuantity(), instruction.getSymbol(), currentPrice, fee);
                
                return TradeExecutionEntity.builder()
                        .instructionId(instructionId)
                        .symbol(instruction.getSymbol())
                        .action(TradeAction.SELL)
                        .quantity(instruction.getQuantity())
                        .price(currentPrice)
                        .fee(fee)
                        .status(ExecutionStatus.FILLED)
                        .executionTime(executionTime)
                        .build();
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Failed to execute instruction: {}", instruction, e);
            
            // 返回失败的执行记录
            return TradeExecutionEntity.builder()
                    .instructionId(instructionId)
                    .symbol(instruction.getSymbol())
                    .action(instruction.getAction())
                    .quantity(instruction.getQuantity())
                    .status(ExecutionStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .executionTime(executionTime)
                    .build();
        }
    }

    private PortfolioSnapshotEntity buildPortfolioSnapshot(VirtualAccountAggregate account) {
        List<PortfolioSnapshotEntity.PositionEntity> positionSnapshots = account.getPositions().values().stream()
                .map(p -> PortfolioSnapshotEntity.PositionEntity.builder()
                        .symbol(p.getSymbol())
                        .quantity(p.getQuantity())
                        .entryPrice(p.getAvgPrice())
                        .unrealizedPnl(BigDecimal.ZERO)
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
