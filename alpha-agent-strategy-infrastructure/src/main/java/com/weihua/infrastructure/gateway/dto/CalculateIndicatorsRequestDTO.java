/*
 * @Author: weihua hu
 * @Date: 2025-11-28 15:34:00
 * @Description: 计算技术指标请求 DTO
 */
package com.weihua.infrastructure.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 计算技术指标请求参数
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CalculateIndicatorsRequestDTO {
    /**
     * 资产代码
     * 格式: "CRYPTO:BTC"
     */
    private String symbol;
    
    /**
     * 数据周期
     * 如: "30d", "90d", "1y"
     */
    @Builder.Default
    private String period = "30d";
    
    /**
     * K线间隔
     * 如: "1d", "1h", "15m"
     */
    @Builder.Default
    private String interval = "1d";
}
