package com.weihua.infrastructure.adapter.provider;

import com.weihua.infrastructure.config.LlmProperties;
import com.weihua.strategy.domain.service.provider.LlmConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

/**
 * 基于 Nacos 配置的 LLM 模型选择提供者
 * 
 * 实现策略：
 * 1. 优先从策略配置中读取用户选择的 modelId
 * 2. 如果策略未指定，使用系统默认模型 (Nacos 配置的 default-provider)
 * 
 * 未来扩展：
 * - 可以从数据库的策略表中读取 model_id 字段
 * - 可以支持策略级别的模型覆盖
 */
@Slf4j
@Service
@RefreshScope
public class NacosLlmConfigProvider implements LlmConfigProvider {

    @Autowired
    private LlmProperties llmProperties;

    // TODO: 注入策略仓储，从数据库读取策略配置的 modelId
    // @Autowired
    // private IStrategyInstanceRepository strategyRepository;

    @Override
    public String getModelId(String strategyId) {
        // TODO: 未来从数据库读取策略配置中的 model_id
        // StrategyInstanceAggregate strategy = strategyRepository.queryStrategyInstance(strategyId);
        // if (strategy != null && strategy.getConfig().getModelId() != null) {
        //     return strategy.getConfig().getModelId();
        // }
        
        // 当前实现：返回 null，让 LlmFactory 使用系统默认模型
        // 这样所有策略都使用 Nacos 配置的 default-model
        String defaultModel = llmProperties.getDefaultModel();
        log.debug("Strategy {} using default model: {}", strategyId, defaultModel);
        
        return defaultModel; // 返回默认模型 ID
    }
}

