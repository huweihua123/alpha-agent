/*
 * @Author: weihua hu
 * @Date: 2025-11-28 15:34:00
 * @Description: Stock MCP 市场数据服务接口
 */
package com.weihua.infrastructure.gateway;

import com.weihua.infrastructure.gateway.dto.BatchPricesRequestDTO;
import com.weihua.infrastructure.gateway.dto.BatchPricesResponseDTO;
import com.weihua.infrastructure.gateway.dto.CalculateIndicatorsRequestDTO;
import com.weihua.infrastructure.gateway.dto.CalculateIndicatorsResponseDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Stock MCP HTTP 服务接口
 * 基于 Retrofit2 实现
 */
public interface IStockMcpService {
    
    /**
     * 批量获取资产价格
     * 
     * @param requestDTO 请求参数
     * @return 批量价格数据
     */
    @POST("/api/v1/market/prices/batch")
    Call<BatchPricesResponseDTO> getBatchPrices(@Body BatchPricesRequestDTO requestDTO);
    
    /**
     * 计算技术指标
     * 
     * @param requestDTO 请求参数
     * @return 技术指标数据
     */
    @POST("/api/v1/market/indicators/calculate")
    Call<CalculateIndicatorsResponseDTO> calculateIndicators(@Body CalculateIndicatorsRequestDTO requestDTO);
}
