package com.weihua.strategy.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSnapshotEntity {
    private BigDecimal totalBalance;
    private BigDecimal availableBalance;
    private List<PositionEntity> positions;
    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionEntity {
        private String symbol;
        private BigDecimal quantity;
        private BigDecimal entryPrice;
        private BigDecimal unrealizedPnl;
    }
}
