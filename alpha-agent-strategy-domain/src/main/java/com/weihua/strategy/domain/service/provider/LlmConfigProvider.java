package com.weihua.strategy.domain.service.provider;

/**
 * LLM 模型选择提供者接口
 * 用于获取策略对应的模型 ID (用户选择的模型标识符)
 * 
 * 设计理念：
 * - 用户/策略只需要指定想用哪个模型 (如 "deepseek", "dashscope")
 * - 具体的 API Key、Base URL 等配置由系统 (Nacos) 统一管理
 */
public interface LlmConfigProvider {
    
    /**
     * 获取指定策略使用的模型 ID
     * @param strategyId 策略 ID
     * @return 模型 ID (如 "deepseek", "dashscope", "google")，如果返回 null 则使用系统默认模型
     */
    String getModelId(String strategyId);
}

