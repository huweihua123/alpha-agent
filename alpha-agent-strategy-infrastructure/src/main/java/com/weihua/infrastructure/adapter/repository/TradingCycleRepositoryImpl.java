/*
 * @Author: weihua hu
 * @Date: 2025-11-28 16:15:00
 * @LastEditTime: 2025-11-28 16:15:00
 * @LastEditors: weihua hu
 * @Description: 交易历史仓储实现
 */
package com.weihua.infrastructure.adapter.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weihua.infrastructure.dao.IDecisionLogDao;
import com.weihua.infrastructure.dao.IMarketHistoryDao;
import com.weihua.infrastructure.dao.IPortfolioSnapshotDao;
import com.weihua.infrastructure.dao.ITradeExecutionDao;
import com.weihua.infrastructure.dao.po.DecisionLogPO;
import com.weihua.infrastructure.dao.po.MarketHistoryPO;
import com.weihua.infrastructure.dao.po.PortfolioSnapshotPO;
import com.weihua.infrastructure.dao.po.TradeExecutionPO;
import com.weihua.strategy.domain.model.entity.DecisionEntity;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.entity.PortfolioSnapshotEntity;
import com.weihua.strategy.domain.model.entity.TradeExecutionEntity;
import com.weihua.strategy.domain.model.aggregate.TradingCycleAggregate;
import com.weihua.strategy.domain.repository.ITradingCycleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 交易历史仓储实现
 * 负责将交易循环中的各类数据持久化到数据库
 */
@Repository
@Slf4j
public class TradingCycleRepositoryImpl implements ITradingCycleRepository {

    @Resource
    private IMarketHistoryDao marketHistoryDao;

    @Resource
    private IDecisionLogDao decisionLogDao;

    @Resource
    private ITradeExecutionDao tradeExecutionDao;

    @Resource
    private IPortfolioSnapshotDao portfolioSnapshotDao;

    @Resource
    private com.weihua.infrastructure.dao.IStrategyInstanceDao strategyInstanceDao;

    @Resource
    private ObjectMapper objectMapper;

    private Long getPhysicalId(String strategyId) {
        com.weihua.infrastructure.dao.po.StrategyInstancePO po = strategyInstanceDao.selectByStrategyId(strategyId);
        if (po == null) {
            throw new IllegalArgumentException("Strategy not found: " + strategyId);
        }
        return po.getId();
    }

    @Override
    public void saveMarketSnapshot(String strategyId, MarketSnapshotEntity snapshot) {
        try {
            Long physicalId = getPhysicalId(strategyId);
            MarketHistoryPO po = MarketHistoryPO.builder()
                    .strategyInstanceId(physicalId)
                    .symbol(snapshot.getSymbol())
                    .price(snapshot.getPrice())
                    .rsi(snapshot.getRsi())
                    .fundingRate(snapshot.getFundingRate())
                    .indicatorsJson(snapshot.getIndicatorsJson())
                    .recordedAt(snapshot.getTimestamp() != null ? snapshot.getTimestamp() : LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            marketHistoryDao.insert(po);
            log.debug("Saved market snapshot for strategy: {}, symbol: {}", strategyId, snapshot.getSymbol());

        } catch (Exception e) {
            log.error("Failed to save market snapshot for strategy: {}", strategyId, e);
            // 不抛出异常，避免影响主流程
        }
    }

    private Long saveDecisionLog(Long strategyId, String cycleId, DecisionEntity decision) {
        try {
            // 将 instructions 转换为 JSON
            String instructionsJson = null;
            if (decision.getInstructions() != null && !decision.getInstructions().isEmpty()) {
                instructionsJson = objectMapper.writeValueAsString(decision.getInstructions());
            }

            // 将 rawPlan 转换为 JSON (作为 llmResponseJson)
            String llmResponseJson = null;
            if (decision.getRawPlan() != null) {
                llmResponseJson = objectMapper.writeValueAsString(decision.getRawPlan());
            }

            DecisionLogPO po = DecisionLogPO.builder()
                    .strategyInstanceId(strategyId)
                    .cycleId(cycleId)
                    .rationale(decision.getRationale())
                    .instructionsJson(instructionsJson)
                    .llmResponseJson(llmResponseJson)
                    .decisionTime(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            decisionLogDao.insert(po);
            log.debug("Saved decision log for strategy: {}, id: {}", strategyId, po.getId());
            return po.getId();

        } catch (Exception e) {
            log.error("Failed to save decision log for strategy: {}", strategyId, e);
            return null;
        }
    }

    private void saveTradeExecution(Long strategyId, Long decisionLogId, TradeExecutionEntity execution) {
        try {
            TradeExecutionPO po = TradeExecutionPO.builder()
                    .strategyInstanceId(strategyId)
                    .decisionLogId(decisionLogId)
                    .instructionId(execution.getInstructionId())
                    .action(execution.getAction().getCode())
                    .symbol(execution.getSymbol())
                    .quantity(execution.getQuantity())
                    .price(execution.getPrice())
                    .fee(execution.getFee())
                    .status(execution.getStatus().getCode())
                    .errorMessage(execution.getErrorMessage())
                    .executionTime(execution.getExecutionTime() != null ? execution.getExecutionTime() : LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            tradeExecutionDao.insert(po);
            log.debug("Saved trade execution for strategy: {}, action: {}, symbol: {}", 
                    strategyId, execution.getAction().getCode(), execution.getSymbol());

        } catch (Exception e) {
            log.error("Failed to save trade execution for strategy: {}", strategyId, e);
        }
    }

    private void savePortfolioSnapshot(Long strategyId, Long decisionLogId, PortfolioSnapshotEntity snapshot) {
        try {
            // 将 positions 转换为 JSON
            String positionsJson = null;
            if (snapshot.getPositions() != null && !snapshot.getPositions().isEmpty()) {
                positionsJson = objectMapper.writeValueAsString(snapshot.getPositions());
            }

            PortfolioSnapshotPO po = PortfolioSnapshotPO.builder()
                    .strategyInstanceId(strategyId)
                    .decisionLogId(decisionLogId)
                    .totalBalance(snapshot.getTotalBalance())
                    .availableBalance(snapshot.getAvailableBalance())
                    .positionsJson(positionsJson)
                    .recordedAt(snapshot.getTimestamp() != null ? snapshot.getTimestamp() : LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            portfolioSnapshotDao.insert(po);
            log.debug("Saved portfolio snapshot for strategy: {}, balance: {}", 
                    strategyId, snapshot.getTotalBalance());

        } catch (Exception e) {
            log.error("Failed to save portfolio snapshot for strategy: {}", strategyId, e);
        }
    }
    @Override
    public void save(TradingCycleAggregate cycle) {
        Long physicalId = getPhysicalId(cycle.getStrategyId());
        
        // 1. 保存市场快照
        if (cycle.getMarketSnapshot() != null) {
            saveMarketSnapshot(cycle.getStrategyId(), cycle.getMarketSnapshot());
        }

        // 2. 保存决策日志
        Long decisionLogId = saveDecisionLog(physicalId, cycle.getCycleId(), cycle.getDecision());
        
        if (decisionLogId != null) {
            // 3. 保存交易执行记录
            if (cycle.getExecutions() != null) {
                for (TradeExecutionEntity execution : cycle.getExecutions()) {
                    saveTradeExecution(physicalId, decisionLogId, execution);
                }
            }
            
            // 4. 保存资产快照
            if (cycle.getPortfolioSnapshot() != null) {
                savePortfolioSnapshot(physicalId, decisionLogId, cycle.getPortfolioSnapshot());
            }
        }
    }
}
