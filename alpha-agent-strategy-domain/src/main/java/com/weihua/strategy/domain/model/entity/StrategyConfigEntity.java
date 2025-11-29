package com.weihua.strategy.domain.model.entity;

import com.weihua.strategy.domain.model.valobj.TradingMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyConfigEntity {
    private String strategyName;
    private String strategyType; // PROMPT, GRID
    private String exchangeId;
    private TradingMode tradingMode;
    private Integer intervalSeconds;
    private String templateId; // Renamed from promptTemplate
    private String promptText;
    private List<String> symbols;
    private String riskLevel;
    private java.math.BigDecimal maxPositionSize;
    private java.math.BigDecimal initialCapital;
    private java.math.BigDecimal leverage;
    private String configJson;
    
    /** 用户选择的模型 ID (如 "deepseek", "dashscope", "google")，如果为 null 则使用系统默认模型 */
    private String modelId;
}

