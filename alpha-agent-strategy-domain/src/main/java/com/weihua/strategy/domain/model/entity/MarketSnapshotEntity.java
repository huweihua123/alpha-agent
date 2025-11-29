package com.weihua.strategy.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketSnapshotEntity {
    private String symbol;
    private BigDecimal price;
    private BigDecimal rsi;
    private BigDecimal fundingRate;
    /** 技术指标 JSON 字符串 */
    private String indicatorsJson;
    private LocalDateTime timestamp;
}
