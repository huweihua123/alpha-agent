package com.weihua.trigger.http;

import com.weihua.strategy.domain.model.aggregate.StrategyInstanceAggregate;
import com.weihua.strategy.domain.model.entity.StrategyConfigEntity;
import com.weihua.strategy.domain.model.valobj.StrategyStatus;
import com.weihua.strategy.domain.model.valobj.TradingMode;
import com.weihua.strategy.domain.repository.IStrategyInstanceRepository;

import com.weihua.types.model.Response;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/strategy")
public class StrategyController {

    @Resource
    private com.weihua.strategy.application.service.TradingCycleAppService tradingCycleAppService;

    @Resource
    private IStrategyInstanceRepository strategyInstanceRepository;

    /**
     * 手动触发一次交易循环
     */
    @PostMapping("/run/{id}")
    public Response<String> runCycle(@PathVariable String id) {
        try {
            tradingCycleAppService.executeCycle(id);
            return Response.success("Cycle executed successfully");
        } catch (Exception e) {
            return Response.error("Failed to execute cycle: " + e.getMessage());
        }
    }

    /**
     * 创建一个新的测试策略
     */
    @Resource
    private com.weihua.strategy.domain.repository.IPromptRepository promptRepository;

    @Resource
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * 创建一个新的策略
     */
    @PostMapping("/create")
    public Response<String> createStrategy(@RequestBody com.weihua.strategy.api.dto.CreateStrategyRequestDTO request) {
        try {
            // 1. 解析 Prompt
            String promptText = request.getPromptText();
            if (request.getTemplateId() != null && !request.getTemplateId().isEmpty()) {
                String templateContent = promptRepository.getTemplateContent(request.getTemplateId());
                if (templateContent != null) {
                    // 如果有模板，优先使用模板内容
                    promptText = templateContent;
                }
            }

            // 2. 提取 modelId
            String modelId = request.getModelId();

            // 3. 构建 Config JSON (Exchange & Trading Extra)
            // 注意：llmConfig 已被移除，不再存入 configJson，因为 modelId 已经作为独立字段存储在 Entity 中
            java.util.Map<String, Object> fullConfig = new java.util.HashMap<>();
            if (request.getExchangeConfig() != null) {
                fullConfig.put("exchangeConfig", request.getExchangeConfig());
            }
            if (request.getTradingConfig() != null) {
                fullConfig.put("tradingConfig", request.getTradingConfig());
            }
            String configJson = objectMapper.writeValueAsString(fullConfig);

            // 4. 构建 StrategyConfigEntity
            com.weihua.strategy.api.dto.TradingConfigDTO tradingConfig = request.getTradingConfig();
            StrategyConfigEntity config = StrategyConfigEntity.builder()
                    .strategyName(request.getStrategyName())
                    .strategyType(request.getStrategyType() != null ? request.getStrategyType() : "PROMPT")
                    .templateId(request.getTemplateId())
                    .promptText(promptText)
                    .modelId(modelId) // 设置用户选择的模型 ID
                    .configJson(configJson)
                    // Trading Config 映射
                    .symbols(tradingConfig != null ? tradingConfig.getSymbols() : List.of())
                    .riskLevel(tradingConfig != null ? tradingConfig.getRiskLevel() : "MEDIUM")
                    .maxPositionSize(tradingConfig != null ? tradingConfig.getMaxPositionSize() : java.math.BigDecimal.ZERO)
                    .initialCapital(tradingConfig != null ? tradingConfig.getInitialCapital() : java.math.BigDecimal.ZERO)
                    .leverage(tradingConfig != null ? tradingConfig.getLeverage() : java.math.BigDecimal.ONE)
                    .intervalSeconds(tradingConfig != null ? tradingConfig.getIntervalSeconds() : 60)
                    .tradingMode(tradingConfig != null && "LIVE".equalsIgnoreCase(tradingConfig.getTradingMode()) ? TradingMode.LIVE : TradingMode.VIRTUAL)
                    .exchangeId(request.getExchangeConfig() != null ? request.getExchangeConfig().getExchangeId() : null)
                    .build();

            // 5. 构建 Aggregate
            StrategyInstanceAggregate strategy = StrategyInstanceAggregate.builder()
                    .userId(request.getUserId())
                    .status(StrategyStatus.RUNNING)
                    .config(config)
                    .build();

            // 6. 保存
            strategyInstanceRepository.save(strategy);
            return Response.success(strategy.getStrategyId());

        } catch (Exception e) {
            return Response.error("Failed to create strategy: " + e.getMessage());
        }
    }

    /**
     * 查询用户的所有策略
     */
    @GetMapping("/list/{userId}")
    public Response<List<StrategyInstanceAggregate>> listUserStrategies(@PathVariable String userId) {
        try {
            List<StrategyInstanceAggregate> strategies = strategyInstanceRepository.findByUserId(userId);
            return Response.success(strategies);
        } catch (Exception e) {
            return Response.error("Failed to list strategies: " + e.getMessage());
        }
    }

    /**
     * 查询用户指定状态的策略
     */
    @GetMapping("/list/{userId}/{status}")
    public Response<List<StrategyInstanceAggregate>> listUserStrategiesByStatus(
            @PathVariable String userId,
            @PathVariable String status) {
        try {
            StrategyStatus strategyStatus = StrategyStatus.valueOf(status.toUpperCase());
            List<StrategyInstanceAggregate> strategies = 
                strategyInstanceRepository.findByUserIdAndStatus(userId, strategyStatus);
            return Response.success(strategies);
        } catch (Exception e) {
            return Response.error("Failed to list strategies: " + e.getMessage());
        }
    }
}
