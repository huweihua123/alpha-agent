package com.weihua.strategy.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 最终决策实体 (Approved Decision)
 * 代表经过风控清洗、确认可执行的最终决策
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionEntity {
    /**
     * 最终决策理由 (可能包含风控修正的说明)
     */
    private String rationale;

    /**
     * 最终指令列表 (已清洗、已拆分、已校验)
     */
    private List<TradeInstruction> instructions;
    
    /**
     * 原始计划 (可选，用于追溯)
     */
    private TradePlan rawPlan;
}
