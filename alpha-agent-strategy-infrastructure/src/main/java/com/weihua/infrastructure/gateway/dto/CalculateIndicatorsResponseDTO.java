/*
 * @Author: weihua hu
 * @Date: 2025-11-28 15:34:00
 * @Description: 计算技术指标响应 DTO
 */
package com.weihua.infrastructure.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 计算技术指标响应数据
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CalculateIndicatorsResponseDTO {
    private String symbol;
    private String timestamp;
    private PriceInfoDTO price;
    private IndicatorsDTO indicators;
    
    /**
     * 价格信息
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PriceInfoDTO {
        private BigDecimal current;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal volume;
    }
    
    /**
     * 技术指标集合
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IndicatorsDTO {
        private SmaDTO sma;
        private EmaDTO ema;
        private RsiDTO rsi;
        private MacdDTO macd;
        @com.fasterxml.jackson.annotation.JsonProperty("bollinger_bands")
        private BollingerBandsDTO bollingerBands;
        private KdjDTO kdj;
        private BigDecimal atr;
    }
    
    /**
     * SMA 指标
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SmaDTO {
        @JsonProperty("sma_20")
        private BigDecimal sma20;
        @JsonProperty("sma_50")
        private BigDecimal sma50;
        @JsonProperty("sma_200")
        private BigDecimal sma200;
    }
    
    /**
     * EMA 指标
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmaDTO {
        @JsonProperty("ema_12")
        private BigDecimal ema12;
        @JsonProperty("ema_26")
        private BigDecimal ema26;
    }
    
    /**
     * RSI 指标
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RsiDTO {
        private BigDecimal value;
        private String signal; // "overbought", "oversold", "neutral"
    }
    
    /**
     * MACD 指标
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MacdDTO {
        private BigDecimal macd;
        private BigDecimal signal;
        private BigDecimal histogram;
    }
    
    /**
     * 布林带指标
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BollingerBandsDTO {
        private BigDecimal upper;
        private BigDecimal middle;
        private BigDecimal lower;
    }
    
    /**
     * KDJ 指标
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KdjDTO {
        private BigDecimal k;
        private BigDecimal d;
    }
}
