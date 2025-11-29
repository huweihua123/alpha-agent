package com.weihua.strategy.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 交易参数配置 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingConfigDTO implements Serializable {
    
    /** 交易对列表 (e.g., ["BTC-USDT", "ETH-USDT"]) */
    private List<String> symbols;
    
    /** 决策循环间隔 (秒)，默认 60 */
    private Integer intervalSeconds;
    
    /** 交易模式 ("VIRTUAL", "LIVE")，默认 "VIRTUAL" */
    private String tradingMode;
    
    /** 初始资金 (仅模拟盘有效) */
    private BigDecimal initialCapital;
    
    /** 杠杆倍数 */
    private BigDecimal leverage;
    
    /** 风险等级 ("LOW", "MEDIUM", "HIGH") */
    private String riskLevel;
    
    /** 最大持仓限制 (USDT) */
    private BigDecimal maxPositionSize;
    
    /** 止损百分比 (e.g., 0.05 表示 5%) */
    private BigDecimal stopLossPercentage;
    
    /** 止盈百分比 */
    private BigDecimal takeProfitPercentage;
}
