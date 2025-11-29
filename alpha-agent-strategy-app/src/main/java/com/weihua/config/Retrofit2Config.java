/*
 * @Author: weihua hu
 * @Date: 2025-11-28 15:34:00
 * @LastEditTime: 2025-11-28 15:34:00
 * @LastEditors: weihua hu
 * @Description: Retrofit2 配置 - Stock MCP 服务
 */
package com.weihua.config;

import com.weihua.infrastructure.gateway.IStockMcpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Retrofit2 HTTP 客户端配置
 * 用于调用 Stock MCP 服务
 */
@Configuration
public class Retrofit2Config {

    @Value("${stock-mcp.base-url:http://localhost:9898}")
    private String stockMcpBaseUrl;

    /**
     * 创建 Stock MCP 服务客户端
     */
    @Bean
    public IStockMcpService stockMcpService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(stockMcpBaseUrl)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        return retrofit.create(IStockMcpService.class);
    }
}
