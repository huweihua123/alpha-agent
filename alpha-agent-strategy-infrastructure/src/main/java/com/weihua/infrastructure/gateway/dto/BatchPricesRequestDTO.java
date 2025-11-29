/*
 * @Author: weihua hu
 * @Date: 2025-11-28 15:34:00
 * @Description: 批量获取价格请求 DTO
 */
package com.weihua.infrastructure.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量获取价格请求参数
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatchPricesRequestDTO {
    /**
     * 资产代码列表
     * 格式: ["CRYPTO:BTC", "CRYPTO:ETH", "NASDAQ:AAPL"]
     */
    private List<String> tickers;
}
