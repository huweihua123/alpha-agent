/*
 * @Author: weihua hu
 * @Date: 2025-11-28 15:34:00
 * @LastEditTime: 2025-11-28 15:34:00
 * @LastEditors: weihua hu
 * @Description: 市场数据端口实现，通过 Stock MCP HTTP 服务获取市场数据
 */
package com.weihua.infrastructure.adapter.port;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weihua.infrastructure.gateway.IStockMcpService;
import com.weihua.infrastructure.gateway.dto.BatchPricesRequestDTO;
import com.weihua.infrastructure.gateway.dto.BatchPricesResponseDTO;
import com.weihua.infrastructure.gateway.dto.CalculateIndicatorsRequestDTO;
import com.weihua.infrastructure.gateway.dto.CalculateIndicatorsResponseDTO;
import com.weihua.strategy.domain.adapter.port.IMarketDataPort;
import com.weihua.strategy.domain.model.entity.MarketSnapshotEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 市场数据端口实现
 * 通过 Stock MCP HTTP 服务获取实时价格和技术指标
 */
@Service
@Slf4j
public class MarketDataPort implements IMarketDataPort {

    @Resource
    private IStockMcpService stockMcpService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, MarketSnapshotEntity> fetchLatestPrices(List<String> symbols) {
        log.info("Fetching latest prices for symbols: {}", symbols);
        
        try {
            // 构建请求参数
            BatchPricesRequestDTO requestDTO = BatchPricesRequestDTO.builder()
                    .tickers(symbols)
                    .build();
            
            // 调用 Stock MCP 服务
            Response<BatchPricesResponseDTO> response = stockMcpService.getBatchPrices(requestDTO).execute();
            
            if (!response.isSuccessful() || response.body() == null) {
                log.error("Failed to fetch prices from Stock MCP, code: {}, message: {}", 
                         response.code(), response.message());
                return getMockPrices(symbols);
            }
            
            // BatchPricesResponseDTO 直接是 Map<String, PriceDataDTO>
            BatchPricesResponseDTO pricesMap = response.body();
            
            if (pricesMap.isEmpty()) {
                log.warn("Empty price data returned from Stock MCP");
                return getMockPrices(symbols);
            }
            
            // 转换为 MarketSnapshotEntity
            Map<String, MarketSnapshotEntity> result = new HashMap<>();
            for (String symbol : symbols) {
                BatchPricesResponseDTO.PriceDataDTO priceData = pricesMap.get(symbol);
                if (priceData != null) {
                    result.put(symbol, MarketSnapshotEntity.builder()
                            .symbol(symbol)
                            .price(priceData.getPrice())
                            .timestamp(parseTimestamp(priceData.getTimestamp()))
                            .build());
                    log.debug("Fetched price for {}: {}", symbol, priceData.getPrice());
                } else {
                    log.warn("No price data for symbol: {}", symbol);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Exception when fetching prices from Stock MCP: {}", e.getMessage(), e);
            // 降级到 Mock 数据
            return getMockPrices(symbols);
        }
    }

    @Override
    public String fetchTechnicalIndicators(String symbol) {
        log.info("Fetching technical indicators for: {}", symbol);
        
        try {
            // 构建请求参数
            CalculateIndicatorsRequestDTO requestDTO = CalculateIndicatorsRequestDTO.builder()
                    .symbol(symbol)
                    .period("30d")
                    .interval("1d")
                    .build();
            
            // 调用 Stock MCP 服务
            Response<CalculateIndicatorsResponseDTO> response = stockMcpService
                    .calculateIndicators(requestDTO).execute();
            
            if (!response.isSuccessful() || response.body() == null) {
                log.error("Failed to fetch indicators from Stock MCP, code: {}, message: {}", 
                         response.code(), response.message());
                return getMockIndicators();
            }
            
            CalculateIndicatorsResponseDTO responseDTO = response.body();
            
            // 转换为 JSON 字符串
            String indicatorsJson = objectMapper.writeValueAsString(responseDTO);
            log.debug("Fetched indicators for {}: {}", symbol, indicatorsJson);
            
            return indicatorsJson;
            
        } catch (Exception e) {
            log.error("Exception when fetching indicators from Stock MCP: {}", e.getMessage(), e);
            // 降级到 Mock 数据
            return getMockIndicators();
        }
    }

    /**
     * 解析时间戳
     * 支持 ISO 8601 格式
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}, using current time", timestamp);
            return LocalDateTime.now();
        }
    }

    /**
     * Mock 价格数据（降级方案）
     */
    private Map<String, MarketSnapshotEntity> getMockPrices(List<String> symbols) {
        log.warn("Using mock prices for symbols: {}", symbols);
        Map<String, MarketSnapshotEntity> result = new HashMap<>();
        for (String symbol : symbols) {
            BigDecimal mockPrice = new BigDecimal("50000.00");
            if (symbol.contains("ETH")) {
                mockPrice = new BigDecimal("3000.00");
            } else if (symbol.contains("AAPL")) {
                mockPrice = new BigDecimal("277.55");
            }
            result.put(symbol, MarketSnapshotEntity.builder()
                    .symbol(symbol)
                    .price(mockPrice)
                    .timestamp(LocalDateTime.now())
                    .build());
        }
        return result;
    }

    /**
     * Mock 技术指标数据（降级方案）
     */
    private String getMockIndicators() {
        log.warn("Using mock technical indicators");
        return "{\"RSI\": 50.0, \"MACD\": 0.0, \"signal\": \"neutral\"}";
    }
}

