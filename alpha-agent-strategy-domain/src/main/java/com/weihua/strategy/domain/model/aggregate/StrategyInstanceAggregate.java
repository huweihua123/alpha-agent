package com.weihua.strategy.domain.model.aggregate;

import com.weihua.strategy.domain.model.entity.StrategyConfigEntity;
import com.weihua.strategy.domain.model.valobj.StrategyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyInstanceAggregate {
    private String strategyId; // Business ID (UUID)
    private String userId;
    private StrategyConfigEntity config;
    private StrategyStatus status;

    public void start() {
        if (this.status == StrategyStatus.RUNNING) {
            throw new IllegalStateException("Strategy is already running");
        }
        this.status = StrategyStatus.RUNNING;
    }

    public void stop() {
        this.status = StrategyStatus.STOPPED;
    }

    public void pause() {
        if (this.status != StrategyStatus.RUNNING) {
            throw new IllegalStateException("Only running strategy can be paused");
        }
        this.status = StrategyStatus.PAUSED;
    }

    public void updateConfig(StrategyConfigEntity newConfig) {
        this.config = newConfig;
    }
}
