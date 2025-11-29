/*
 * @Author: weihua hu
 * @Date: 2025-11-28 15:34:00
 * @Description: 批量获取价格响应 DTO
 */
package com.weihua.infrastructure.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * 批量获取价格响应数据
 * 直接继承 HashMap，响应格式: Map<Ticker, PriceData>
 * 
 * 示例响应:
 * {
 *   "CRYPTO:BTC": { "ticker": "CRYPTO:BTC", "price": 91506.164, ... },
 *   "CRYPTO:ETH": { "ticker": "CRYPTO:ETH", "price": 3021.0457, ... }
 * }
 */
public class BatchPricesResponseDTO extends HashMap<String, BatchPricesResponseDTO.PriceDataDTO> {
    
    /**
     * 单个资产的价格数据
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PriceDataDTO {
        private String ticker;
        private BigDecimal price;
        private String currency;
        private String timestamp;
        private BigDecimal volume;
        
        @JsonProperty("open_price")
        private BigDecimal openPrice;
        
        @JsonProperty("high_price")
        private BigDecimal highPrice;
        
        @JsonProperty("low_price")
        private BigDecimal lowPrice;
        
        @JsonProperty("close_price")
        private BigDecimal closePrice;
        
        @JsonProperty("change")
        private BigDecimal change;
        
        @JsonProperty("change_percent")
        private BigDecimal changePercent;
        
        @JsonProperty("market_cap")
        private BigDecimal marketCap;
        
        private String source;
    }
}
