package com.weihua.strategy.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 创建策略请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStrategyRequestDTO implements Serializable {
    
    /** 用户 ID */
    private String userId;
    
    /** 策略名称 */
    private String strategyName;
    
    /** 策略类型 (默认 "PROMPT") */
    private String strategyType;
    
    /** 用户输入的自然语言策略描述 */
    private String promptText;
    
    /** 使用的 Prompt 模板 ID */
    private String templateId;
    
    /** 
     * 用户选择的模型 ID (如 "deepseek-v3", "gpt-4o")
     * 如果为 null，则使用系统默认模型
     */
    private String modelId;
    
    /** 交易所配置 */
    private ExchangeConfigDTO exchangeConfig;
    
    /** 交易参数配置 */
    private TradingConfigDTO tradingConfig;
}

