package com.weihua.strategy.domain.model.aggregate;

import com.weihua.strategy.domain.model.entity.DecisionEntity;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.entity.PortfolioSnapshotEntity;
import com.weihua.strategy.domain.model.entity.TradeExecutionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingCycleAggregate {
    private String cycleId;
    private String strategyId;
    private MarketSnapshotEntity marketSnapshot;
    private PortfolioSnapshotEntity portfolioSnapshot;
    private DecisionEntity decision;
    private List<TradeExecutionEntity> executions;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public void addExecution(TradeExecutionEntity execution) {
        if (this.executions == null) {
            this.executions = new java.util.ArrayList<>();
        }
        this.executions.add(execution);
    }

    public void recordDecision(DecisionEntity decision) {
        this.decision = decision;
    }

    public void finish(PortfolioSnapshotEntity snapshot) {
        this.portfolioSnapshot = snapshot;
        this.endTime = LocalDateTime.now();
    }
}
