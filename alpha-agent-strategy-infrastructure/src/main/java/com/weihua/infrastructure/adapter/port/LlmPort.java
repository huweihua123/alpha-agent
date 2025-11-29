/*
 * @Author: weihua hu
 * @Date: 2025-11-28 16:10:00
 * @LastEditTime: 2025-11-28 16:10:00
 * @LastEditors: weihua hu
 * @Description: LLM 决策端口实现，通过 ChatClient 调用 LLM 获取交易决策
 */
package com.weihua.infrastructure.adapter.port;

import com.alibaba.cloud.ai.agent.nacos.NacosOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weihua.infrastructure.config.StrategyAgentPromptConfig;
import com.weihua.strategy.domain.adapter.port.ILlmPort;
import com.weihua.strategy.domain.model.entity.DecisionEntity;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.entity.PortfolioSnapshotEntity;
import com.weihua.strategy.domain.model.entity.StrategyConfigEntity;
import com.weihua.strategy.domain.model.entity.TradePlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 决策端口实现
 * 使用 ChatClient 调用 LLM 进行交易决策
 */
@Service
@Slf4j
public class LlmPort implements ILlmPort {

    @Autowired
    private com.weihua.infrastructure.adapter.factory.LlmFactory llmFactory;

    @Autowired
    private com.weihua.strategy.domain.service.provider.LlmConfigProvider llmConfigProvider;

    @Autowired(required = false)
    private StrategyAgentPromptConfig promptConfig;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 从 Nacos 加载的 Prompt 模板缓存
     */
    private volatile String cachedPromptTemplate = null;

    @Override
    public TradePlan askForPlan(
            String strategyId,
            MarketSnapshotEntity market,
            PortfolioSnapshotEntity portfolio,
            StrategyConfigEntity config
    ) {
        log.info("Requesting LLM plan for strategy: {}, symbol: {}", 
                config.getStrategyName(), market.getSymbol());

        try {
            // 1. 获取模型 ID (从策略配置或使用默认)
            String modelId = llmConfigProvider.getModelId(strategyId);
            
            // 2. 动态构建 ChatModel (LlmFactory 会从 Nacos 查找配置)
            org.springframework.ai.chat.model.ChatModel chatModel = llmFactory.createChatModel(modelId);
            
            // 3. 加载 Prompt 模板
            String promptTemplate = loadPromptTemplate();

            // 4. 构建完整的 Prompt
            String fullPrompt = buildPrompt(promptTemplate, market, portfolio, config);
            
            log.debug("LLM Prompt:\n{}", fullPrompt);

            // 5. 调用 ChatModel
            // 使用 ChatClient 包装 ChatModel 以获得更流畅的 API (可选，或者直接调用 chatModel.call)
            // 这里直接调用 chatModel
            String response = chatModel.call(fullPrompt);

            log.debug("LLM Response:\n{}", response);

            // 6. 解析 JSON 响应
            TradePlan plan = parsePlan(response);
            
            log.info("LLM Plan: rationale={}, instructions_count={}", 
                    plan.getRationale(), 
                    plan.getInstructions() != null ? plan.getInstructions().size() : 0);

            return plan;

        } catch (Exception e) {
            log.error("Failed to get LLM plan: {}", e.getMessage(), e);
            // 降级到保守决策
            return getFallbackPlan(config);
        }
    }

    /**
     * 加载 Prompt 模板
     * 优先从 Nacos 加载，降级到本地文件
     */
    private String loadPromptTemplate() {
        // TODO: 未来可以集成 Nacos 动态配置
        // 目前使用本地文件
        if (promptConfig != null && promptConfig.getStrategyAgentInstruction() != null) {
            try {
                String template = promptConfig.getStrategyAgentInstruction()
                        .getContentAsString(StandardCharsets.UTF_8);
                log.debug("Loaded prompt template from local file");
                return template;
            } catch (Exception e) {
                log.warn("Failed to load prompt from local file: {}", e.getMessage());
            }
        }

        // 降级到默认模板
        log.warn("Using default prompt template");
        return getDefaultPromptTemplate();
    }

    /**
     * 构建完整的 Prompt
     */
    private String buildPrompt(
            String template,
            MarketSnapshotEntity market,
            PortfolioSnapshotEntity portfolio,
            StrategyConfigEntity config
    ) {
        // 构建市场数据描述
        String marketData = buildMarketDataDescription(market);
        
        // 构建持仓组合描述
        String portfolioData = buildPortfolioDescription(portfolio);
        
        // 构建策略配置描述
        String strategyConfig = buildStrategyConfigDescription(config);

        // 替换模板变量
        return template
                .replace("{market_data}", marketData)
                .replace("{portfolio}", portfolioData)
                .replace("{strategy_config}", strategyConfig);
    }

    /**
     * 构建市场数据描述
     */
    private String buildMarketDataDescription(MarketSnapshotEntity market) {
        StringBuilder sb = new StringBuilder();
        sb.append("Symbol: ").append(market.getSymbol()).append("\n");
        sb.append("Current Price: ").append(market.getPrice()).append("\n");
        sb.append("Timestamp: ").append(market.getTimestamp()).append("\n");
        
        // 添加技术指标（如果有）
        if (market.getIndicatorsJson() != null && !market.getIndicatorsJson().isEmpty()) {
            sb.append("\nTechnical Indicators:\n");
            sb.append(formatIndicatorsJson(market.getIndicatorsJson()));
        }
        
        return sb.toString();
    }

    /**
     * 格式化技术指标 JSON 为可读文本
     */
    private String formatIndicatorsJson(String indicatorsJson) {
        try {
            Map<String, Object> indicators = objectMapper.readValue(indicatorsJson, Map.class);
            StringBuilder sb = new StringBuilder();
            
            // 提取关键指标
            Map<String, Object> indicatorsData = (Map<String, Object>) indicators.get("indicators");
            if (indicatorsData != null) {
                // RSI
                Map<String, Object> rsi = (Map<String, Object>) indicatorsData.get("rsi");
                if (rsi != null) {
                    sb.append("- RSI: ").append(rsi.get("value"))
                      .append(" (").append(rsi.get("signal")).append(")\n");
                }
                
                // MACD
                Map<String, Object> macd = (Map<String, Object>) indicatorsData.get("macd");
                if (macd != null) {
                    sb.append("- MACD: ").append(macd.get("macd"))
                      .append(", Signal: ").append(macd.get("signal"))
                      .append(", Histogram: ").append(macd.get("histogram")).append("\n");
                }
                
                // SMA
                Map<String, Object> sma = (Map<String, Object>) indicatorsData.get("sma");
                if (sma != null) {
                    sb.append("- SMA20: ").append(sma.get("sma_20"))
                      .append(", SMA50: ").append(sma.get("sma_50"))
                      .append(", SMA200: ").append(sma.get("sma_200")).append("\n");
                }
                
                // EMA
                Map<String, Object> ema = (Map<String, Object>) indicatorsData.get("ema");
                if (ema != null) {
                    sb.append("- EMA12: ").append(ema.get("ema_12"))
                      .append(", EMA26: ").append(ema.get("ema_26")).append("\n");
                }
            }
            
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to format indicators JSON: {}", e.getMessage());
            return indicatorsJson;
        }
    }

    /**
     * 构建持仓组合描述
     */
    private String buildPortfolioDescription(PortfolioSnapshotEntity portfolio) {
        StringBuilder sb = new StringBuilder();
        sb.append("Total Balance: ").append(portfolio.getTotalBalance()).append(" USDT\n");
        sb.append("Available Balance: ").append(portfolio.getAvailableBalance()).append(" USDT\n");
        
        if (portfolio.getPositions() != null && !portfolio.getPositions().isEmpty()) {
            sb.append("\nCurrent Positions:\n");
            for (PortfolioSnapshotEntity.PositionEntity position : portfolio.getPositions()) {
                sb.append("- ").append(position.getSymbol())
                  .append(": Quantity=").append(position.getQuantity())
                  .append(", Entry Price=").append(position.getEntryPrice())
                  .append("\n");
            }
        } else {
            sb.append("\nNo current positions.\n");
        }
        
        return sb.toString();
    }

    /**
     * 构建策略配置描述
     */
    private String buildStrategyConfigDescription(StrategyConfigEntity config) {
        StringBuilder sb = new StringBuilder();
        sb.append("Strategy Name: ").append(config.getStrategyName()).append("\n");
        sb.append("Symbols: ").append(String.join(", ", config.getSymbols())).append("\n");
        sb.append("Max Position Size: ").append(config.getMaxPositionSize()).append("\n");
        sb.append("Risk Level: ").append(config.getRiskLevel()).append("\n");
        
        return sb.toString();
    }

    /**
     * 解析 LLM 响应为 TradePlan
     */
    private TradePlan parsePlan(String response) {
        try {
            // 提取 JSON 部分（LLM 可能返回带有额外文字的响应）
            String jsonStr = extractJson(response);
            
            if (jsonStr == null || jsonStr.isEmpty()) {
                log.warn("No valid JSON found in LLM response");
                return TradePlan.builder()
                        .rationale("Failed to parse LLM response")
                        .instructions(new ArrayList<>())
                        .build();
            }

            // 解析 JSON
            return objectMapper.readValue(jsonStr, TradePlan.class);

        } catch (Exception e) {
            log.error("Failed to parse LLM plan: {}", e.getMessage(), e);
            return TradePlan.builder()
                    .rationale("Parse error: " + e.getMessage())
                    .instructions(new ArrayList<>())
                    .build();
        }
    }

    /**
     * 从响应中提取 JSON
     */
    private String extractJson(String response) {
        // 尝试找到 JSON 代码块
        Pattern codeBlockPattern = Pattern.compile("```json\\s*([\\s\\S]*?)```", Pattern.MULTILINE);
        Matcher matcher = codeBlockPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 尝试找到纯 JSON 对象
        Pattern jsonPattern = Pattern.compile("\\{[\\s\\S]*\\}", Pattern.MULTILINE);
        matcher = jsonPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }

        // 如果整个响应看起来像 JSON，直接返回
        String trimmed = response.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        return null;
    }

    /**
     * 获取降级计划（保守策略：观望）
     */
    private TradePlan getFallbackPlan(StrategyConfigEntity config) {
        log.warn("Using fallback plan (HOLD) for strategy: {}", config.getStrategyName());
        
        return TradePlan.builder()
                .rationale("Fallback plan due to LLM error - conservative HOLD")
                .instructions(new ArrayList<>()) // 空指令列表表示观望
                .build();
    }

    /**
     * 获取默认 Prompt 模板
     */
    private String getDefaultPromptTemplate() {
        return """
            You are a professional cryptocurrency trading strategy agent.
            
            ## Current Market Data
            {market_data}
            
            ## Current Portfolio
            {portfolio}
            
            ## Strategy Configuration
            {strategy_config}
            
            ## Task
            Analyze the market data and technical indicators, then provide a trading decision.
            
            ## Output Format
            Return your decision in JSON format:
            {
              "rationale": "Brief explanation of your decision",
              "instructions": [
                {
                  "action": "BUY or SELL or HOLD",
                  "symbol": "Trading symbol",
                  "quantity": "Trade quantity (number)",
                  "rationale": "Specific reason for this instruction"
                }
              ]
            }
            
            ## Rules
            1. If you decide to wait, return empty "instructions": []
            2. "quantity" must be a number without units
            3. "action" can only be BUY, SELL, or HOLD
            4. Base your decision on technical indicators (RSI, MACD, EMA, SMA)
            5. Consider current positions to avoid overtrading
            6. Be conservative and protect capital
            """;
    }
}

