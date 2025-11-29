package com.weihua.strategy.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 交易所配置 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeConfigDTO implements Serializable {
    
    /** 交易所 ID (e.g., "binance", "okx") */
    private String exchangeId;
    
    /** API Access Key */
    private String accessKey;
    
    /** API Secret Key */
    private String secretKey;
    
    /** API Passphrase (部分交易所需要) */
    private String passphrase;
    
    /** 是否使用沙箱/模拟环境 */
    private Boolean sandbox;
}
