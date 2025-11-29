package com.weihua.strategy.domain.service;

import com.weihua.strategy.domain.adapter.port.IMarketDataPort;
import com.weihua.strategy.domain.model.aggregate.StrategyInstanceAggregate;
import com.weihua.strategy.domain.model.aggregate.VirtualAccountAggregate;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import com.weihua.strategy.domain.model.valobj.MarketContext;
import com.weihua.strategy.domain.model.valobj.StrategyStatus;
import com.weihua.strategy.domain.repository.IStrategyInstanceRepository;
import com.weihua.strategy.domain.repository.IVirtualAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 领域服务：行情分析
 * 负责准备决策所需的上下文数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketAnalysisDomainService {

    private final IStrategyInstanceRepository strategyInstanceRepository;
    private final IVirtualAccountRepository virtualAccountRepository;
    private final IMarketDataPort marketDataPort;

    /**
     * 准备市场上下文
     */
    public MarketContext prepareContext(String strategyId) {
        // 1. 加载策略实例
        StrategyInstanceAggregate strategy = strategyInstanceRepository.findByStrategyId(strategyId);
        if (strategy == null || strategy.getStatus() != StrategyStatus.RUNNING) {
            log.warn("Strategy {} is not running or not found.", strategyId);
            return null;
        }

        // 2. 加载虚拟账户 (默认 USDT)
        VirtualAccountAggregate account = virtualAccountRepository.findByStrategyId(strategyId, "USDT");
        if (account == null) {
            // 初始化默认账户 (仅用于测试/容错)
            account = VirtualAccountAggregate.builder()
                    .strategyId(strategyId)
                    .currency("USDT")
                    .balance(strategy.getConfig().getInitialCapital() != null ? strategy.getConfig().getInitialCapital() : BigDecimal.ZERO)
                    .frozen(BigDecimal.ZERO)
                    .positions(new HashMap<>())
                    .build();
        }

        // 3. 获取市场数据
        List<String> symbols = strategy.getConfig().getSymbols();
        if (symbols == null || symbols.isEmpty()) {
            log.warn("Strategy {} has no symbols configured.", strategyId);
            return null;
        }

        Map<String, MarketSnapshotEntity> marketData = marketDataPort.fetchLatestPrices(symbols);
        
        // 获取主要交易对的市场数据 (假设第一个为主要)
        String primarySymbol = symbols.get(0);
        MarketSnapshotEntity primaryMarket = marketData.get(primarySymbol);
        
        // 获取技术指标
        if (primaryMarket != null) {
            String indicators = marketDataPort.fetchTechnicalIndicators(primarySymbol);
            primaryMarket.setIndicatorsJson(indicators);
        } else {
            log.warn("Failed to fetch market data for primary symbol: {}", primarySymbol);
            return null;
        }

        return MarketContext.builder()
                .strategyId(strategyId)
                .marketData(marketData)
                .primaryMarket(primaryMarket)
                .account(account)
                .build();
    }
}
