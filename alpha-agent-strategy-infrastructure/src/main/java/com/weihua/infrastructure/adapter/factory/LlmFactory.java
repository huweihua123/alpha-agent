package com.weihua.infrastructure.adapter.factory;

import com.weihua.infrastructure.config.LlmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * LLM 模型工厂
 * 根据 modelId 从 Nacos 配置中查找并构建 ChatModel 实例
 * 
 * 设计理念：系统内置模型配置，用户只需选择 modelId
 */
@Slf4j
@Component
public class LlmFactory {

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private LlmProperties llmProperties;

    /**
     * 根据 modelId 创建 ChatModel
     * @param modelId 模型标识符 (如 "deepseek-v3", "gpt-4o")，如果为 null 则使用默认模型
     * @return ChatModel 实例
     */
    public ChatModel createChatModel(String modelId) {
        // 1. 确定使用哪个 modelId
        String targetModelId = (modelId != null && !modelId.isEmpty()) 
                ? modelId 
                : llmProperties.getDefaultModel();
        
        log.info("Creating ChatModel for modelId: {}", targetModelId);
        
        // Debug: 打印当前加载的所有模型
        if (llmProperties.getModels() != null) {
            log.info("Available models in config: {}", llmProperties.getModels().stream()
                    .map(LlmProperties.ModelConfig::getId)
                    .collect(java.util.stream.Collectors.joining(", ")));
        } else {
            log.warn("Configured models list is NULL");
        }
        
        // 2. 查找 ModelConfig
        LlmProperties.ModelConfig modelConfig = llmProperties.getModels().stream()
                .filter(m -> m.getId().equals(targetModelId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown model ID: " + targetModelId));
        
        // 3. 查找引用的 ProviderConfig
        String providerRef = modelConfig.getProviderRef();
        LlmProperties.ProviderConfig providerConfig = llmProperties.getProviders().get(providerRef);
        if (providerConfig == null) {
            throw new IllegalArgumentException("Model " + targetModelId + " references unknown provider: " + providerRef);
        }
        
        // 4. 根据 providerType 构建对应的 ChatModel
        String providerType = providerConfig.getType();
        if ("openai".equalsIgnoreCase(providerType)) {
            return createOpenAiChatModel(providerConfig, modelConfig);
        }
        
        // TODO: 支持其他 Provider Type
        throw new IllegalArgumentException("Unsupported provider type: " + providerType);
    }

    /**
     * 构建 OpenAI 兼容的 ChatModel
     * 结合 Provider 的连接信息和 Model 的参数信息
     */
    private ChatModel createOpenAiChatModel(LlmProperties.ProviderConfig providerConfig, LlmProperties.ModelConfig modelConfig) {
        // 连接信息来自 Provider
        String apiKey = providerConfig.getApiKey();
        String baseUrl = providerConfig.getBaseUrl();
        
        // 模型参数来自 Model
        String modelName = modelConfig.getModelName();
        Double temperature = modelConfig.getTemperature();
        
        // 验证必要参数
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API Key is required for provider type: " + providerConfig.getType());
        }
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("Base URL is required for provider type: " + providerConfig.getType());
        }
        
        log.debug("Building OpenAI-compatible ChatModel: baseUrl={}, model={}", baseUrl, modelName);
        
        // 构建 OpenAiApi
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(new SimpleApiKey(apiKey))
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();
        
        // 构建 ChatOptions
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelName)
                .temperature(temperature != null ? temperature : 0.7)
                .build();

        // 构建并返回 ChatModel
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
}
