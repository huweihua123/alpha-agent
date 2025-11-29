package com.weihua.strategy.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 交易计划 (Raw Plan)
 * 代表 LLM 返回的原始决策提案，尚未经过风控清洗
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradePlan {
    /**
     * 整体决策理由
     */
    private String rationale;

    /**
     * 原始指令列表
     */
    private List<TradeInstruction> instructions;
}
