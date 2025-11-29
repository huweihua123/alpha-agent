package com.weihua.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * LLM 配置属性类
 * 
 * 采用 Provider 与 Model 分离的设计：
 * - providers: 定义连接通道 (怎么连)，包含 API Key, Base URL 等
 * - models: 定义逻辑模型 (用什么)，包含模型名称、参数等，并引用具体的 provider
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {
    
    /** 默认使用的模型 ID (对应 models 列表中的 id) */
    private String defaultModel = "deepseek-v3";

    /** 
     * 渠道配置集合
     * Key: providerId (e.g., "openai-official", "deepseek-official") 
     */
    private Map<String, ProviderConfig> providers = new java.util.HashMap<>();
    
    /** 
     * 模型配置列表
     * 定义用户可选的模型
     */
    private List<ModelConfig> models = new java.util.ArrayList<>();

    /**
     * 渠道配置 (Connection Layer)
     */
    @Data
    public static class ProviderConfig {
        /** 渠道类型: openai, dashscope, etc. */
        private String type = "openai";
        private String apiKey;
        private String baseUrl;
    }

    /**
     * 模型配置 (Capability Layer)
     */
    @Data
    public static class ModelConfig {
        /** 模型唯一标识符 (用户选择的 ID)，如 "deepseek-v3" */
        private String id;
        
        /** 显示名称，如 "DeepSeek V3" */
        private String displayName;
        
        /** 引用的 providerId */
        private String providerRef;
        
        /** 实际传递给 API 的模型名称，如 "deepseek-chat" */
        private String modelName;
        
        /** 默认温度参数 */
        private Double temperature = 0.7;
    }
}

