package com.weihua.strategy.domain.model.entity;

import com.weihua.strategy.domain.model.valobj.TradeAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 交易指令实体
 * 代表一个具体的、可执行的交易动作
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeInstruction {
    /**
     * 指令唯一ID (UUID)
     */
    private String instructionId;

    /**
     * 交易动作 (BUY, SELL)
     */
    private TradeAction action;

    /**
     * 标的物 (Symbol)
     */
    private String symbol;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 理由/解释
     */
    private String rationale;

    /**
     * 元数据 (存储原始请求量、置信度、中间计算结果等)
     * e.g., "requested_target_qty", "confidence", "original_action"
     */
    private Map<String, Object> meta;
}
