package com.weihua.strategy.domain.adapter.port;

import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;

import java.util.List;
import java.util.Map;

/**
 * 市场数据端口
 * 负责与外部系统（如 MCP）交互获取市场数据
 */
public interface IMarketDataPort {
    /**
     * 批量获取最新价格
     * @param symbols 交易对列表，如 ["BTC/USDT", "ETH/USDT"]
     * @return Map<Symbol, Price>
     */
    Map<String, MarketSnapshotEntity> fetchLatestPrices(List<String> symbols);

    /**
     * 获取技术指标 (RSI, MACD, EMA等)
     * @param symbol 交易对
     * @return JSON String (直接返回 Python 端计算好的 JSON)
     */
    String fetchTechnicalIndicators(String symbol);
}
