package com.weihua.infrastructure.adapter.repository;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weihua.infrastructure.dao.IStrategyInstanceDao;
import com.weihua.infrastructure.dao.po.StrategyInstancePO;
import com.weihua.strategy.domain.model.aggregate.StrategyInstanceAggregate;
import com.weihua.strategy.domain.model.entity.StrategyConfigEntity;
import com.weihua.strategy.domain.model.valobj.StrategyStatus;
import com.weihua.strategy.domain.model.valobj.TradingMode;
import com.weihua.strategy.domain.repository.IStrategyInstanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class StrategyInstanceRepositoryImpl implements IStrategyInstanceRepository {

    @Resource
    private IStrategyInstanceDao strategyInstanceDao;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public StrategyInstanceAggregate findByStrategyId(String strategyId) {
        StrategyInstancePO po = strategyInstanceDao.selectByStrategyId(strategyId);
        if (po == null) {
            return null;
        }
        return convertToAggregate(po);
    }

    @Override
    public void save(StrategyInstanceAggregate aggregate) {
        if (aggregate.getStrategyId() == null) {
            aggregate.setStrategyId(java.util.UUID.randomUUID().toString());
        }

        StrategyInstancePO existingPo = strategyInstanceDao.selectByStrategyId(aggregate.getStrategyId());
        
        if (existingPo == null) {
            StrategyInstancePO newPo = convertToPO(aggregate);
            strategyInstanceDao.insert(newPo);
            // After insert, newPo.getId() is populated if needed, but Aggregate doesn't store it anymore.
        } else {
            // Update existing
            StrategyInstancePO updatePo = convertToPO(aggregate);
            updatePo.setId(existingPo.getId()); // Keep the physical ID
            strategyInstanceDao.updateStatusByStrategyId(aggregate.getStrategyId(), aggregate.getStatus().name());
            strategyInstanceDao.updateConfigByStrategyId(aggregate.getStrategyId(), updatePo.getConfigJson());
            // TODO: If we want to update other fields like promptText, we need a full update method in DAO.
            // For now, assuming status and configJson are the main mutable parts during runtime.
            // If metadata changes, we might need a more comprehensive update.
        }
    }

    @Override
    public List<StrategyInstanceAggregate> findByStatus(StrategyStatus status) {
        List<StrategyInstancePO> poList = strategyInstanceDao.selectByStatus(status.name());
        if (poList == null || poList.isEmpty()) {
            return new ArrayList<>();
        }
        return poList.stream()
                .map(this::convertToAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public void updateStatus(String strategyId, StrategyStatus status) {
        strategyInstanceDao.updateStatusByStrategyId(strategyId, status.name());
    }

    @Override
    public List<StrategyInstanceAggregate> findByUserId(String userId) {
        List<StrategyInstancePO> poList = strategyInstanceDao.selectByUserId(userId);
        if (poList == null || poList.isEmpty()) {
            return new ArrayList<>();
        }
        return poList.stream()
                .map(this::convertToAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyInstanceAggregate> findByUserIdAndStatus(String userId, StrategyStatus status) {
        List<StrategyInstancePO> poList = strategyInstanceDao.selectByUserIdAndStatus(userId, status.name());
        if (poList == null || poList.isEmpty()) {
            return new ArrayList<>();
        }
        return poList.stream()
                .map(this::convertToAggregate)
                .collect(Collectors.toList());
    }

    private StrategyInstanceAggregate convertToAggregate(StrategyInstancePO po) {
        if (po == null) {
            return null;
        }

        // 解析 configJson (这里我们只简单地将其传回，具体解析由使用者按需进行，或者在这里统一解析)
        // 目前 StrategyConfigEntity 已经有了 configJson 字段
        
        StrategyConfigEntity config = StrategyConfigEntity.builder()
                .strategyName(po.getStrategyName())
                .strategyType(po.getStrategyType())
                .exchangeId(po.getExchangeId())
                .tradingMode(po.getTradingMode() != null ? TradingMode.valueOf(po.getTradingMode()) : null) // Handle null for TradingMode
                .intervalSeconds(po.getIntervalSeconds())
                .templateId(po.getTemplateId())
                .promptText(po.getPromptText())
                .configJson(po.getConfigJson()) // Map configJson
                .build();

        // 尝试从 configJson 中恢复部分字段 (如果 PO 字段为空但 JSON 中有)
        // 或者反之，PO 字段优先。目前 PO 字段是主要来源。
        
        // 补充从 configJson 解析出的字段 (symbols, riskLevel 等)
        if (po.getConfigJson() != null) {
            try {
                JsonNode root = objectMapper.readTree(po.getConfigJson());
                if (root.has("tradingConfig")) {
                    JsonNode trading = root.get("tradingConfig");
                    if (trading.has("symbols")) {
                        List<String> symbols = objectMapper.convertValue(trading.get("symbols"), new TypeReference<List<String>>(){});
                        config.setSymbols(symbols);
                    }
                    if (trading.has("riskLevel")) config.setRiskLevel(trading.get("riskLevel").asText());
                    if (trading.has("maxPositionSize")) config.setMaxPositionSize(new BigDecimal(trading.get("maxPositionSize").asText()));
                    if (trading.has("initialCapital")) config.setInitialCapital(new BigDecimal(trading.get("initialCapital").asText()));
                    if (trading.has("leverage")) config.setLeverage(new BigDecimal(trading.get("leverage").asText()));
                }
            } catch (Exception e) {
                log.warn("Failed to parse configJson for strategy {}", po.getStrategyId(), e);
            }
        }

        return StrategyInstanceAggregate.builder()
                .strategyId(po.getStrategyId())
                .userId(po.getUserId())
                .status(StrategyStatus.valueOf(po.getStatus()))
                .config(config)
                .build();
    }

    private StrategyInstancePO convertToPO(StrategyInstanceAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }
        StrategyConfigEntity config = aggregate.getConfig();
        
        StrategyInstancePO po = new StrategyInstancePO();
        po.setStrategyId(aggregate.getStrategyId());
        po.setUserId(aggregate.getUserId());
        po.setStatus(aggregate.getStatus().name());
        
        if (config != null) {
            po.setStrategyName(config.getStrategyName());
            po.setStrategyType(config.getStrategyType());
            po.setExchangeId(config.getExchangeId());
            po.setTradingMode(config.getTradingMode() != null ? config.getTradingMode().name() : null); // Handle null for TradingMode
            po.setIntervalSeconds(config.getIntervalSeconds());
            po.setTemplateId(config.getTemplateId());
            po.setPromptText(config.getPromptText());
            po.setConfigJson(config.getConfigJson()); // Map configJson
        }
        
        return po;
    }

    private StrategyConfigEntity convertConfig(String configJson) {
        if (configJson == null) {
            return null;
        }
        return JSON.parseObject(configJson, StrategyConfigEntity.class);
    }
}
